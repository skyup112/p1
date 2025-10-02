package com.example.p1.service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Add these two import statements for the enums
import com.example.p1.domain.TeamType;
import com.example.p1.domain.PlayerRole;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 롯데 자이언츠 공식 웹사이트에서 경기 일정 및 상세 데이터(선수 라인업, 댓글)를 크롤링하는 서비스 (Selenium 기반).
 * 실제 브라우저를 통해 동적인 콘텐츠를 로드하고 파싱합니다.
 */
@Service
public class KboGameCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(KboGameCrawlerService.class);

    private static final String LOTTE_GIANTS_BASE_URL = "https://www.giantsclub.com";
    private static final String LOTTE_GIANTS_DETAIL_PAGE_BASE = LOTTE_GIANTS_BASE_URL + "/html/?pcode=257&type=calendar&flag=1&gmkey=";
    private static final String LOTTE_GIANTS_SCHEDULE_PAGE_BASE = LOTTE_GIANTS_BASE_URL + "/html/?pcode=257&type=calendar";

    // 롯데 자이언츠 웹사이트의 날짜 형식에 맞는 DateTimeFormatter 정의 (예: 2025.07.01)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    // 최종 LocalDateTime 형식 (ISO_LOCAL_DATE_TIME과 동일)
    private static final DateTimeFormatter OUTPUT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final Map<String, String> TEAM_CODES = new HashMap<>();
    private static final Map<String, String> TEAM_NAME_TO_SHORT_NAME_MAP = new HashMap<>();

    // Optional: If you're manually managing chromedriver, uncomment and configure this.
    // @Value("${webdriver.chrome.driver:}") // Default empty string if not set
    // private String chromeDriverPath;

    static {
        TEAM_CODES.put("롯데", "LT");
        TEAM_CODES.put("두산", "OB");
        TEAM_CODES.put("NC", "NC");
        TEAM_CODES.put("LG", "LG");
        TEAM_CODES.put("KT", "KT");
        TEAM_CODES.put("SSG", "SK"); // SSG 랜더스
        TEAM_CODES.put("키움", "WO"); // 키움 히어로즈
        TEAM_CODES.put("삼성", "SS");
        TEAM_CODES.put("한화", "HH");
        TEAM_CODES.put("KIA", "HT");

        TEAM_NAME_TO_SHORT_NAME_MAP.put("롯데 자이언츠", "롯데");
        TEAM_NAME_TO_SHORT_NAME_MAP.put("SSG 랜더스", "SSG");
        TEAM_NAME_TO_SHORT_NAME_MAP.put("두산 베어스", "두산");
        TEAM_NAME_TO_SHORT_NAME_MAP.put("키움 히어로즈", "키움");
        TEAM_NAME_TO_SHORT_NAME_MAP.put("LG 트윈스", "LG");
        TEAM_NAME_TO_SHORT_NAME_MAP.put("KT 위즈", "KT");
        TEAM_NAME_TO_SHORT_NAME_MAP.put("NC 다이노스", "NC");
        TEAM_NAME_TO_SHORT_NAME_MAP.put("삼성 라이온즈", "삼성");
        TEAM_NAME_TO_SHORT_NAME_MAP.put("한화 이글스", "한화");
        TEAM_NAME_TO_SHORT_NAME_MAP.put("KIA 타이거즈", "KIA");
    }

    // If you uncommented @Value, uncomment this too:
    // @PostConstruct
    // public void init() {
    //     if (!chromeDriverPath.isEmpty()) {
    //         System.setProperty("webdriver.chrome.driver", chromeDriverPath);
    //         log.info("ChromeDriver path set to: {}", chromeDriverPath);
    //     } else {
    //         log.info("webdriver.chrome.driver property not set. Relying on Selenium Manager.");
    //     }
    // }

    private String getTeamCode(String fullTeamName) {
        String shortName = TEAM_NAME_TO_SHORT_NAME_MAP.getOrDefault(fullTeamName, "Unknown");
        return TEAM_CODES.getOrDefault(shortName, "XX");
    }

    private String getShortTeamName(String fullTeamName) {
        return TEAM_NAME_TO_SHORT_NAME_MAP.getOrDefault(fullTeamName, "Unknown");
    }

    /**
     * Configures and creates a ChromeDriver instance.
     * @return A new WebDriver instance.
     */
    private WebDriver createWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run in background without UI
        options.addArguments("--disable-gpu"); // Recommended for headless
        options.addArguments("--no-sandbox"); // Recommended for Linux/Docker
        options.addArguments("--window-size=1920,1080"); // Set a consistent window size
        options.addArguments("--disable-dev-shm-usage"); // Mitigate resource issues
        options.addArguments("--remote-allow-origins=*"); // Allow remote origins (might be needed in some environments)

        // Add user-agent to mimic a real browser to avoid simple bot detection
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        log.debug("WebDriver initialized successfully by Selenium Manager.");
        return driver;
    }

    /**
     * 특정 연도와 월의 롯데 자이언츠 경기 일정을 크롤링합니다.
     * 이 메서드는 Selenium WebDriver를 사용하여 실제 웹 페이지를 탐색하고 데이터를 가져옵니다.
     *
     * @param year 크롤링할 연도
     * @param intMonth 크롤링할 월 (1-12)
     * @return 각 경기의 상세 정보가 담긴 맵 리스트
     * @throws IOException 웹 크롤링 중 오류 발생 시
     */
    public List<Map<String, String>> crawlKboSchedule(int year, int intMonth) throws IOException {
        List<Map<String, String>> allGames = new ArrayList<>();
        String currentYear = String.valueOf(year);
        String month = String.format("%02d", intMonth); // 01, 02 형식으로 포맷

        log.info("Starting Selenium crawl for Lotte Giants game schedule for {}-{}...", currentYear, month);

        WebDriver driver = null;
        try {
            driver = createWebDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30)); // 명시적 대기 시간 30초 설정

            String url = LOTTE_GIANTS_SCHEDULE_PAGE_BASE + "&y=" + currentYear + "&m=" + month;
            log.info("Navigating to: {}", url);
            driver.get(url);

            // 페이지의 메인 컨텐츠 영역이 로드될 때까지 기다립니다.
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("div_score_cal")));
                log.info("Page main content (#div_score_cal) is present.");
            } catch (org.openqa.selenium.TimeoutException e) {
                log.error("Timeout waiting for main page content (#div_score_cal) to load. Check URL or network issues. URL: {}", url);
                throw new IOException("Failed to load the main schedule page content.", e);
            }

            // 년도 선택 (dropdown) - 이미 URL에 포함되어 있어 선택이 불필요할 수 있으나, 혹시 모를 로딩 문제 방지
            // 또는 페이지가 AJAX로 데이터를 로드할 경우, 드롭다운을 통한 재로딩이 필요할 수 있습니다.
            try {
                WebElement yearDropdown = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("year")));
                Select yearSelect = new Select(yearDropdown);
                if (!yearSelect.getFirstSelectedOption().getAttribute("value").equals(currentYear)) {
                    yearSelect.selectByValue(currentYear);
                    log.info("Selected year: {}", currentYear);
                } else {
                    log.debug("Year {} already selected.", currentYear);
                }
            } catch (NoSuchElementException e) {
                log.warn("Year dropdown not found. Assuming URL parameter is sufficient or page structure changed.");
            }

            // 월 선택 (dropdown)
            try {
                WebElement monthDropdown = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("month")));
                Select monthSelect = new Select(monthDropdown);
                if (!monthSelect.getFirstSelectedOption().getAttribute("value").equals(month)) {
                    monthSelect.selectByValue(month);
                    log.info("Selected month: {}", month);

                    // 드롭다운 변경 후, form submit이 발생하므로, 페이지가 다시 로드되거나 AJAX 로딩 완료까지 대기 (강화된 대기 로직)
                    // .tbl-score는 여전히 사용되므로, 이 요소의 stalenessOf와 presenceOfElementLocated를 사용합니다.
                    try {
                        WebElement oldTable = driver.findElement(By.cssSelector(".tbl-score"));
                        wait.until(ExpectedConditions.stalenessOf(oldTable));
                        log.info("Old table has become stale after year/month selection.");
                    } catch (org.openqa.selenium.NoSuchElementException e) {
                        log.debug("Initial .tbl-score element not found for staleness check or it disappeared quickly. Proceeding.");
                    } catch (Exception e) {
                        log.warn("Error during staleness check for .tbl-score after year/month selection: {}", e.getMessage());
                    }
                    // 새로운 .tbl-score가 나타날 때까지 대기
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".tbl-score")));
                    log.info("New .tbl-score element is present after year/month selection.");
                } else {
                    log.debug("Month {} already selected.", month);
                }
            } catch (NoSuchElementException e) {
                log.warn("Month dropdown not found. Assuming URL parameter is sufficient or page structure changed.");
            }

            // 추가적으로, 새로운 월의 첫 번째 경기 링크가 '보일' 때까지 기다립니다. (가장 중요)
            // 경기 결과가 표시되는 a.score-re 링크를 기준으로 합니다.
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".tbl-score td a.score-re, .tbl-score td.none"))); // score-re가 없으면 none 클래스도 체크
            log.info("Page content reloaded after month selection and game links are visible or no games are present.");

            // 경기 링크 요소들을 찾습니다.
            List<WebElement> gameLinkElements = driver.findElements(By.cssSelector(".tbl-score td a.score-re"));
            log.info("Found {} game links using Selenium.", gameLinkElements.size());

            if (gameLinkElements.isEmpty()) {
                log.warn("No game links found for {}-{} . This month might have no games or selectors are incorrect.", currentYear, month);
                // Check for "경기가 없습니다" message
                try {
                    WebElement noGameMessage = driver.findElement(By.cssSelector(".tbl-score td.none"));
                    if (noGameMessage.isDisplayed() && noGameMessage.getText().contains("경기가 없습니다")) {
                        log.info("Confirmed: '경기가 없습니다' message found for {}-{}", currentYear, month);
                        return Collections.emptyList(); // Return empty list as there are no games
                    }
                } catch (NoSuchElementException e) {
                    log.debug("No '경기가 없습니다' message found. Proceeding as if there might be games or a different issue.");
                }
            }

            Pattern gameKeyDatePattern = Pattern.compile("^(\\d{8})"); // YYYYMMDD 추출 패턴 (gmkey 시작 부분)

            for (WebElement linkElement : gameLinkElements) {
                Map<String, String> gameData = new HashMap<>();
                String gameKey = "";

                try {
                    String gameLinkHref = linkElement.getAttribute("href");
                    Pattern gameKeyExtractPattern = Pattern.compile("gmkey=([^&]+)");
                    Matcher gameKeyMatcher = gameKeyExtractPattern.matcher(gameLinkHref);
                    if (gameKeyMatcher.find()) {
                        gameKey = gameKeyMatcher.group(1);
                        gameData.put("gameKey", gameKey); // gameKey 저장
                    } else {
                        log.warn("Warning: Game key not found for link: {}. Skipping this game.", gameLinkHref);
                        continue;
                    }

                    // game_key에서 년월일 정보 추출 (YYYYMMDD)
                    String gameDateFromKey = "";
                    Matcher dateMatcher = gameKeyDatePattern.matcher(gameKey);
                    if (dateMatcher.find()) {
                        gameDateFromKey = dateMatcher.group(1);
                    } else {
                        log.warn("Warning: Could not parse date from gameKey: {}. Skipping this game.", gameKey);
                        continue;
                    }

                    String parsedYear = gameDateFromKey.substring(0, 4);
                    String parsedMonth = gameDateFromKey.substring(4, 6);
                    String parsedDay = gameDateFromKey.substring(6, 8);

                    String formattedDate = String.format("%s.%s.%s", parsedYear, parsedMonth, parsedDay);
                    LocalDate gameLocalDate = null;
                    try {
                        gameLocalDate = LocalDate.parse(formattedDate, DATE_FORMATTER);
                        gameData.put("formattedDate", formattedDate);

                        String koreanDayOfWeek = gameLocalDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
                        String rawDate = String.format("%s년 %s월 %s일 (%s)",
                                parsedYear, parsedMonth, Integer.parseInt(parsedDay), koreanDayOfWeek);
                        gameData.put("rawDate", rawDate);

                    } catch (DateTimeParseException e) {
                        log.error("Error parsing date '{}' from gameKey: {}. Skipping this game.", formattedDate, e.getMessage());
                        continue;
                    }

                    // 각 경기 요소에서 정보 추출
                    WebElement vsAtSpan = linkElement.findElement(By.cssSelector(".va"));
                    String vsAt = vsAtSpan.getText().trim();

                    WebElement opponentImg = linkElement.findElement(By.cssSelector("img"));
                    String opponentTeamShortName = opponentImg.getAttribute("alt").trim();
                    String opponentLogoUrlRaw = opponentImg.getAttribute("src");
                    String opponentLogoUrl = "N/A";
                    if (!opponentLogoUrlRaw.isEmpty()) {
                        if (opponentLogoUrlRaw.startsWith("/")) {
                            opponentLogoUrl = LOTTE_GIANTS_BASE_URL + opponentLogoUrlRaw;
                        } else {
                            opponentLogoUrl = opponentLogoUrlRaw;
                        }
                    }
                    gameData.put("opponentLogoUrl", opponentLogoUrl);

                    WebElement scoreSpan = null;
                    try {
                        scoreSpan = linkElement.findElement(By.cssSelector(".score .sco"));
                    } catch (org.openqa.selenium.NoSuchElementException e) {
                        // score span이 없을 수 있음 (경기 예정 등)
                        log.debug("Score span not found for game {}. Assuming not yet finished.", gameKey);
                    }
                    String scoreText = scoreSpan != null ? scoreSpan.getText().trim() : "";

                    WebElement winLoseImg = null;
                    try {
                        winLoseImg = linkElement.findElement(By.cssSelector(".score img"));
                    } catch (org.openqa.selenium.NoSuchElementException e) {
                        // 승패 이미지가 없을 수 있음
                        log.debug("Win/lose image not found for game {}. Assuming not yet finished.", gameKey);
                    }
                    String winLoseStatus = winLoseImg != null ? winLoseImg.getAttribute("alt").trim() : "";


                    WebElement placeSpan = linkElement.findElement(By.cssSelector(".place"));
                    String placeText = placeSpan.getText().trim();

                    String stadium = placeText;
                    String gameTime = "N/A";
                    Pattern placeTimePattern = Pattern.compile("(.+?)(\\d{2}:\\d{2})$");
                    Matcher placeTimeMatcher = placeTimePattern.matcher(placeText);
                    if (placeTimeMatcher.find()) {
                        stadium = placeTimeMatcher.group(1).trim();
                        gameTime = placeTimeMatcher.group(2).trim();
                    }
                    gameData.put("gameTime", gameTime);
                    gameData.put("stadium", stadium); // 경기장 정보는 "stadium" 키로 저장

                    LocalDateTime fullGameDateTime = null;
                    try {
                        fullGameDateTime = LocalDateTime.of(gameLocalDate, java.time.LocalTime.parse(gameTime, DateTimeFormatter.ofPattern("HH:mm")));
                    } catch (DateTimeParseException e) {
                        log.error("Error parsing time '{}' for game {}: {}. Setting to midnight.", gameTime, gameKey, e.getMessage());
                        fullGameDateTime = LocalDateTime.of(gameLocalDate, LocalTime.of(0, 0)); // 기본값 설정
                    }
                    gameData.put("gameDateTime", fullGameDateTime.format(OUTPUT_DATETIME_FORMATTER));

                    String homeTeamActualShortName;
                    String awayTeamActualShortName;
                    String homeAwayIndicator;
                    if ("vs".equalsIgnoreCase(vsAt)) {
                        homeTeamActualShortName = "롯데";
                        awayTeamActualShortName = opponentTeamShortName;
                        homeAwayIndicator = "Home";
                    } else if ("at".equalsIgnoreCase(vsAt)) {
                        homeTeamActualShortName = opponentTeamShortName;
                        awayTeamActualShortName = "롯데";
                        homeAwayIndicator = "Away";
                    } else {
                        homeTeamActualShortName = "롯데"; // 기본적으로 롯데를 홈팀으로 간주
                        awayTeamActualShortName = opponentTeamShortName;
                        homeAwayIndicator = "Unknown";
                        log.warn("Warning: Unknown vs/at indicator '{}' for game {}", vsAt, gameKey);
                    }
                    gameData.put("homeTeamShortName", homeTeamActualShortName);
                    gameData.put("awayTeamShortName", awayTeamActualShortName);
                    gameData.put("vsAtIndicator", vsAt);
                    gameData.put("homeAway", homeAwayIndicator);
                    gameData.put("opponentTeam", opponentTeamShortName);

                    gameData.put("homeScore", "0");
                    gameData.put("awayScore", "0");
                    gameData.put("status", "SCHEDULED");
                    gameData.put("score", scoreText);
                    gameData.put("winLoseStatus", winLoseStatus);

                    if (scoreText.contains(":")) {
                        String[] scores = scoreText.split(":");
                        try {
                            // Ensure proper score assignment based on vs/at
                            if ("vs".equalsIgnoreCase(vsAt)) { // Lotte is Home
                                gameData.put("homeScore", scores[0].trim()); // Lotte's score
                                gameData.put("awayScore", scores[1].trim()); // Opponent's score
                            } else { // Lotte is Away (at)
                                gameData.put("homeScore", scores[0].trim()); // Opponent's score
                                gameData.put("awayScore", scores[1].trim()); // Lotte's score
                            }
                            gameData.put("status", "FINISHED");
                        } catch (NumberFormatException e) {
                            log.error("Error parsing scores '{}' for game {}: {}. Setting scores to 0.", scoreText, gameKey, e.getMessage());
                        }
                    } else if ("취소".equals(scoreText)) {
                        gameData.put("status", "CANCELED");
                    }

                    gameData.put("gameLink", LOTTE_GIANTS_BASE_URL + gameLinkHref); // 상대 경로를 절대 경로로 변환
                    gameData.put("linkTitle", linkElement.getAttribute("title"));

                    gameData.put("match", String.format("롯데 %s %s", vsAt, opponentTeamShortName));

                    String remark = "";
                    if ("FINISHED".equals(gameData.get("status"))) {
                        remark = String.format("Score: %s, Result: %s", scoreText, winLoseStatus);
                    } else if ("CANCELED".equals(gameData.get("status"))) {
                        remark = "경기 취소";
                    } else {
                        remark = "경기 예정";
                    }
                    gameData.put("remark", remark);

                    allGames.add(gameData);
                    log.info("Successfully parsed game: {} {} {} (Status: {})", gameData.get("formattedDate"), gameData.get("gameTime"), gameData.get("match"), gameData.get("status"));

                } catch (Exception e) {
                    log.error("Error processing individual game link (GameKey: {}): {}. Skipping this game.", gameKey, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("CRITICAL ERROR during Selenium schedule crawling process: {}. Full stack trace below:", e.getMessage(), e);
            throw new IOException("Failed to crawl game schedules due to a critical Selenium or page interaction error.", e);
        } finally {
            if (driver != null) {
                driver.quit();
                log.info("WebDriver closed for schedule crawl.");
            }
        }
        log.info("Finished crawling schedule. Total games parsed: {}", allGames.size());
        return allGames;
    }


    /**
     * 특정 경기의 출전 선수 데이터를 크롤링합니다.
     * 이 메서드는 gameKey를 직접 받으므로, crawlKboSchedule에서 추출된 gameKey를 사용해야 합니다.
     *
     * @param gameKey 경기 고유 키 (YYYYMMDD + AwayTeamCode + HomeTeamCode + Sequence)
     * @param homeTeamFullname 홈 팀의 전체 이름
     * @param awayTeamFullname 원정 팀의 전체 이름
     * @return 각 선수에 대한 정보를 담은 맵의 리스트
     */
    public List<Map<String, Object>> crawlGamePlayersForGame(String gameKey, String homeTeamFullname, String awayTeamFullname) {
        String giantsUrl = LOTTE_GIANTS_DETAIL_PAGE_BASE + gameKey;

        WebDriver driver = null;
        List<Map<String, Object>> scrapedPlayers = new ArrayList<>();

        log.info("Attempting to crawl player lineups for gameKey: {} at URL: {}", gameKey, giantsUrl);

        try {
            driver = createWebDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            driver.get(giantsUrl);
            log.debug("Navigated to game detail URL: {}", driver.getCurrentUrl());

            // Check if the game detail page content is present
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".score-info-wrap")));
            log.debug("Game detail page (.score-info-wrap) is present.");


            String homeTeamShortName = getShortTeamName(homeTeamFullname);
            String awayTeamShortName = getShortTeamName(awayTeamFullname);

            log.debug("Identified homeTeamShortName: {}, awayTeamShortName: {}", homeTeamShortName, awayTeamShortName);

            // Selectors based on provided screenshots (캡처5.PNG, 캡처6.PNG, 캡처7.PNG, 캡처8.PNG, 캡처9.PNG, 캡처10.PNG)
            // They appear to be structured as:
            // h4[text containing team short name]
            //   p.result-record-com[text containing "선발 라인업" or "팀 타자 기록"]
            //   table (this is the target table)

            // Dynamic XPath for Batter Tables (선발 라인업 바로 뒤 테이블)
            By homeBatterTableSelector = By.xpath("//h4[contains(text(), '" + homeTeamShortName + "')]//following-sibling::p[contains(@class, 'result-record-com') and (contains(text(), '선발 라인업') or contains(text(), '타자 기록'))]//following-sibling::table[1]");
            By awayBatterTableSelector = By.xpath("//h4[contains(text(), '" + awayTeamShortName + "')]//following-sibling::p[contains(@class, 'result-record-com') and (contains(text(), '선발 라인업') or contains(text(), '타자 기록'))]//following-sibling::table[1]");

            // Dynamic XPath for Pitcher Tables (투수 기록 바로 뒤 테이블)
            By homePitcherTableSelector = By.xpath("//h4[contains(text(), '" + homeTeamShortName + " 투수 기록')]//following-sibling::table[1]");
            By awayPitcherTableSelector = By.xpath("//h4[contains(text(), '" + awayTeamShortName + " 투수 기록')]//following-sibling::table[1]");

            // Process each table
            processTable(driver, wait, homeBatterTableSelector, homeTeamFullname, TeamType.HOME, PlayerRole.BATTER, scrapedPlayers, gameKey);
            processTable(driver, wait, awayBatterTableSelector, awayTeamFullname, TeamType.AWAY, PlayerRole.BATTER, scrapedPlayers, gameKey);
            processTable(driver, wait, homePitcherTableSelector, homeTeamFullname, TeamType.HOME, PlayerRole.PITCHER, scrapedPlayers, gameKey);
            processTable(driver, wait, awayPitcherTableSelector, awayTeamFullname, TeamType.AWAY, PlayerRole.PITCHER, scrapedPlayers, gameKey);

        } catch (Exception e) {
            log.error("CRITICAL ERROR: An unexpected error occurred during Lotte Giants game players crawl for gmkey {}. Error: {}", gameKey, e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
                log.info("WebDriver closed for Lotte Giants game players crawl for gmkey {}.", gameKey);
            }
        }

        log.info("Finished game players crawl for gmkey {}. Total players found: {}", gameKey, scrapedPlayers.size());
        return scrapedPlayers;
    }

    /**
     * 지정된 셀렉터로 테이블을 찾아 선수 데이터를 추출하고 scrapedPlayers 리스트에 추가합니다.
     */
    private void processTable(WebDriver driver, WebDriverWait wait, By selector, String fullTeamName, TeamType teamType, PlayerRole playerRole, List<Map<String, Object>> scrapedPlayers, String gameKey) {
        try {
            log.debug("Attempting to find table for Team: {}, Role: {} using selector: {}", fullTeamName, playerRole.name(), selector.toString());
            // 테이블이 로드될 때까지 기다림. visibilityOfElementLocated를 사용하여 테이블이 화면에 보이는 상태까지 대기
            WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(selector));
            log.debug("Found and visible table for Team: {}, Role: {}", fullTeamName, playerRole.name());

            WebElement tbody = table.findElement(By.tagName("tbody"));
            List<Map<String, String>> playersInTable = extractPlayersFromRecordTable(tbody, playerRole);

            for (Map<String, String> player : playersInTable) {
                Map<String, Object> playerEntry = new HashMap<>();
                playerEntry.put("gameKey", gameKey); // Add gameKey to player entry
                playerEntry.put("teamName", fullTeamName);
                playerEntry.put("teamType", teamType.name());
                playerEntry.put("playerRole", playerRole.name());
                playerEntry.putAll(player);
                scrapedPlayers.add(playerEntry);
            }
            log.info("Extracted {} {} players for {} from table.", playersInTable.size(), playerRole.name(), fullTeamName);

        } catch (org.openqa.selenium.TimeoutException e) {
            log.warn("Timeout waiting for table for Team: {}, Role: {} (gmkey: {}). This might mean no data is available for this section or selector is incorrect. Error: {}", fullTeamName, playerRole.name(), gameKey, e.getMessage());
        } catch (NoSuchElementException e) {
            log.warn("Table element not found for Team: {}, Role: {} (gmkey: {}). This might mean no data is available for this section or selector is incorrect. Error: {}", fullTeamName, playerRole.name(), gameKey, e.getMessage());
        } catch (Exception e) {
            log.error("Error processing table for Team: {}, Role: {} (gmkey: {}). Error type: {}. Message: {}", fullTeamName, playerRole.name(), gameKey, e.getClass().getName(), e.getMessage(), e);
        }
    }


    /**
     * 경기 기록 테이블의 tbody에서 선수 정보를 추출합니다.
     * 제공된 캡처 이미지(캡처13.PNG: 타자 기록, 캡처3, 캡처4, 캡처5, 캡처15.PNG: 투수 기록)의 HTML 구조를 기반으로 셀 인덱스를 조정했습니다.
     *
     * @param recordTbody 경기 기록 테이블의 tbody WebElement
     * @param playerRole 선수의 역할 (BATTER/PITCHER)
     * @return 추출된 선수 정보 리스트 (이름, 타순/이닝, 포지션 등)
     */
    private List<Map<String, String>> extractPlayersFromRecordTable(WebElement recordTbody, PlayerRole playerRole) {
        List<Map<String, String>> playersData = new ArrayList<>();

        List<WebElement> playerRows = recordTbody.findElements(By.tagName("tr"));
        log.debug("Found {} rows in record table for role {}", playerRows.size(), playerRole.name());

        for (WebElement row : playerRows) {
            try {
                Map<String, String> player = new HashMap<>();
                String playerName = "";

                if (playerRole == PlayerRole.BATTER) {
                    List<WebElement> cells = row.findElements(By.tagName("td"));
                    // 캡처 이미지 (캡처6.PNG)에서 타자 테이블 구조를 보면,
                    // 첫 번째 <td>는 이미지, 두 번째 <td>가 타순, 세 번째 <td>가 선수명입니다.
                    if (cells.size() >= 3) {
                        player.put("orderNumber", cells.get(1).getText().trim()); // 타순
                        playerName = cells.get(2).getText().trim(); // 선수명
                        player.put("position", "타자"); // 타자이므로 포지션 고정
                        player.put("innings", null); // 타자이므로 이닝은 null
                        log.debug("BATTER row parsed: Order={}, Name='{}', Pos='{}'", player.get("orderNumber"), playerName, player.get("position"));
                    } else {
                        log.warn("Not enough cells ({}) for BATTER row. Expected at least 3 relevant cells. Row HTML: {}", cells.size(), row.getAttribute("outerHTML"));
                        continue;
                    }
                } else if (playerRole == PlayerRole.PITCHER) {
                    // 캡처 이미지 (캡처5.PNG)에서 투수 테이블 구조를 보면,
                    // <th>에 선수명, 그 다음 <td>들이 기록. 이닝은 6번째 <td> (인덱스 5) 입니다.
                    WebElement thElement = null;
                    try {
                        thElement = row.findElement(By.tagName("th")); // <th> 태그에 선수 이름이 있음
                    } catch (NoSuchElementException ignored) {
                        // <th>가 없으면 이 행은 스킵. 투수는 <th>에 이름이 있다고 가정
                        log.debug("No <th> element found in pitcher row. Skipping.");
                        continue;
                    }

                    List<WebElement> cells = row.findElements(By.tagName("td"));

                    if (thElement != null && cells.size() >= 6) { // <th> + 최소 6개의 <td> (이닝까지)
                        playerName = thElement.getText().trim(); // 선수명은 <th>에서 가져옴
                        player.put("innings", cells.get(5).getText().trim()); // 이닝 (6번째 <td>, 인덱스 5)
                        player.put("orderNumber", null); // 투수는 타순이 없으므로 null
                        player.put("position", "투수"); // 투수는 포지션을 '투수'로 고정
                        log.debug("PITCHER row parsed: Name='{}', Innings='{}'", playerName, player.get("innings"));
                    } else {
                        log.warn("Not enough cells ({}) or missing <th> for PITCHER row. Expected at least 6 <td> cells and 1 <th>. Row HTML: {}", cells.size(), row.getAttribute("outerHTML"));
                        continue;
                    }
                }

                if (!playerName.isEmpty()) {
                    player.put("playerName", playerName);
                    playersData.add(player);
                } else {
                    log.warn("Player name not found (empty) in row. Skipping row. PlayerRole: {}, Row HTML: {}", playerRole.name(), row.getAttribute("outerHTML"));
                }

            } catch (NoSuchElementException e) {
                log.debug("Expected element not found in row for playerRole {}. Skipping row. Error: {}", playerRole.name(), e.getMessage());
            } catch (Exception e) {
                log.error("Error extracting player data from table row for playerRole {}: {}. Row HTML: {}", playerRole.name(), row.getAttribute("outerHTML"), e);
            }
        }
        return playersData;
    }


    /**
     * 특정 경기의 웹사이트 댓글을 크롤링합니다.
     * 댓글 섹션의 CSS 셀렉터를 사용하여 정보를 추출합니다.
     *
     * @param gameKey 경기 고유 키
     * @param gameDate 경기 날짜 (로깅을 위해 사용)
     * @param homeTeamFullname 홈 팀의 전체 이름 (로깅을 위해 사용)
     * @param awayTeamFullname 원정 팀의 전체 이름 (로깅을 위해 사용)
     * @return 각 댓글에 대한 정보를 담은 맵의 리스트
     * @throws IOException 크롤링 중 IO 오류 발생 시
     */
    public List<Map<String, String>> crawlGameCommentsForGame(String gameKey, LocalDate gameDate, String homeTeamFullname, String awayTeamFullname) throws IOException {
        String giantsUrl = LOTTE_GIANTS_DETAIL_PAGE_BASE + gameKey;
        List<Map<String, String>> comments = new ArrayList<>();
        WebDriver driver = null;

        log.info("Attempting to crawl comments for gameKey: {} (Date: {}, Home: {}, Away: {}) at URL: {}", gameKey, gameDate, homeTeamFullname, awayTeamFullname, giantsUrl);

        try {
            driver = createWebDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            driver.get(giantsUrl);
            log.debug("Navigated to game detail URL for comments: {}", driver.getCurrentUrl());

            // 페이지의 메인 컨텐츠 영역이 로드될 때까지 기다립니다.
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".score-info-wrap")));
            log.debug("Game detail page (.score-info-wrap) is present for comment crawl.");

            // 댓글 영역이 로드될 때까지 기다립니다.
            // 캡처 이미지 (캡처9.PNG, 캡처10.PNG)에 따르면 댓글 목록은 '.board-view-wrap .board-comment-item' 입니다.
            // 댓글이 없을 경우 '.board-view-wrap .board-comment-list li.no-data' 같은 요소가 나타날 수 있습니다.
            By commentListSelector = By.cssSelector(".board-comment-list .board-comment-item");
            By noCommentSelector = By.cssSelector(".board-comment-list li.no-data");

            try {
                // 댓글 항목이 하나라도 나타나거나, "댓글이 없습니다" 메시지가 나타날 때까지 기다립니다.
                wait.until(ExpectedConditions.or(
                        ExpectedConditions.presenceOfElementLocated(commentListSelector),
                        ExpectedConditions.presenceOfElementLocated(noCommentSelector)
                ));
                log.debug("Comment section elements (or no-data message) are present.");

                // "댓글이 없습니다" 메시지 확인
                List<WebElement> noCommentElements = driver.findElements(noCommentSelector);
                if (!noCommentElements.isEmpty() && noCommentElements.get(0).isDisplayed() && noCommentElements.get(0).getText().contains("등록된 댓글이 없습니다")) {
                    log.info("No comments found for gameKey: {}. Message: '{}'", gameKey, noCommentElements.get(0).getText());
                    return Collections.emptyList(); // 댓글이 없음을 확인하고 빈 리스트 반환
                }

                // 실제 댓글 항목들 찾기
                List<WebElement> commentElements = driver.findElements(commentListSelector);
                if (commentElements.isEmpty()) {
                    log.warn("Comment elements selector found no items for gameKey: {}. This might mean structure changed or truly no comments.", gameKey);
                }


                for (WebElement commentElement : commentElements) {
                    Map<String, String> commentData = new HashMap<>();
                    String author = "익명"; // Default if not found
                    String commentText = "";
                    String timestamp = null;

                    try {
                        // 작성자: .user-name
                        WebElement authorElement = commentElement.findElement(By.cssSelector(".user-name"));
                        author = authorElement.getText().trim();
                    } catch (NoSuchElementException e) {
                        log.warn("Author name element (.user-name) not found for a comment in gameKey: {}. Using '익명'. Comment HTML: {}", gameKey, commentElement.getAttribute("outerHTML"));
                    }

                    try {
                        // 댓글 내용: .comment-text
                        WebElement textElement = commentElement.findElement(By.cssSelector(".comment-text"));
                        commentText = textElement.getText().trim();
                    } catch (NoSuchElementException e) {
                        log.warn("Comment text element (.comment-text) not found for a comment in gameKey: {}. Comment HTML: {}", gameKey, commentElement.getAttribute("outerHTML"));
                    }

                    try {
                        // 시간 정보: .date
                        WebElement dateElement = commentElement.findElement(By.cssSelector(".date"));
                        timestamp = dateElement.getText().trim();
                    } catch (NoSuchElementException e) {
                        log.warn("Timestamp element (.date) not found for a comment in gameKey: {}. No timestamp will be stored. Comment HTML: {}", gameKey, commentElement.getAttribute("outerHTML"));
                    }

                    commentData.put("author", author);
                    commentData.put("commentText", commentText);
                    commentData.put("timestamp", timestamp); // may be null

                    if (!commentText.isEmpty()) { // Only add if there's actual comment text
                        comments.add(commentData);
                        log.debug("Scraped comment: Author='{}', Text='{}', Timestamp='{}'", author, commentText, timestamp);
                    } else {
                        log.warn("Skipping empty comment text for gameKey: {}. Comment HTML: {}", gameKey, commentElement.getAttribute("outerHTML"));
                    }
                }
            } catch (org.openqa.selenium.TimeoutException e) {
                log.warn("Timeout waiting for comment list or no-data message for gameKey: {}. Assuming no comments are present.", gameKey);
                return Collections.emptyList(); // Timeout means no comments loaded
            }


        } catch (Exception e) {
            log.error("CRITICAL ERROR during comment crawl for gameKey {}: {}. Full stack trace below:", gameKey, e.getMessage(), e);
            throw new IOException("Failed to crawl comments due to a critical Selenium or page interaction error.", e);
        } finally {
            if (driver != null) {
                driver.quit();
                log.info("WebDriver closed for comment crawl for gameKey {}.", gameKey);
            }
        }
        log.info("Finished comment crawl for gameKey {}. Total comments found: {}", gameKey, comments.size());
        return comments;
    }
}