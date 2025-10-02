
package com.example.p1.service; // 패키지명을 프로젝트 구조에 맞게 변경

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KBO 웹사이트에서 실시간 팀 순위 데이터를 크롤링하는 서비스.
 */
@Service
public class KboCrawlerService {

    /**
     * KBO 웹사이트에서 현재 시즌의 팀 순위 데이터를 크롤링하여 반환합니다.
     * @return 각 팀의 순위 정보가 담긴 맵 리스트. 각 맵은 "rank", "teamName", "games", "wins", "losses", "draws", "winRate", "gamesBehind" 키를 가집니다.
     * @throws IOException 웹 크롤링 중 오류 발생 시
     */
    public List<Map<String, String>> crawlCurrentKboTeamRanks() throws IOException {
        String url = "https://www.koreabaseball.com/Record/TeamRank/TeamRankDaily.aspx";
        Document doc = Jsoup.connect(url).get();

        Element table = doc.selectFirst("table.tData");
        if (table == null) {
            throw new IOException("KBO 순위 테이블을 찾을 수 없습니다. 웹사이트 구조가 변경되었을 수 있습니다.");
        }
        Elements rows = table.select("tbody > tr");

        List<Map<String, String>> crawledRanks = new ArrayList<>();

        for (Element row : rows) {
            Elements cols = row.select("td");
            // 최소한의 열 개수 확인 (순위, 팀명, 경기수, 승, 패, 무, 승률, 게임차)
            if (cols.size() < 8) {
                System.err.println("Warning: Insufficient columns in row, skipping: " + row.text());
                continue;
            }

            Map<String, String> teamData = new HashMap<>();
            try {
                teamData.put("rank", cols.get(0).text().trim());
                teamData.put("teamName", cols.get(1).text().trim());
                teamData.put("games", cols.get(2).text().trim());
                teamData.put("wins", cols.get(3).text().trim());
                teamData.put("losses", cols.get(4).text().trim());
                teamData.put("draws", cols.get(5).text().trim());
                teamData.put("winRate", cols.get(6).text().trim());
                teamData.put("gamesBehind", cols.get(7).text().trim()); // 'GAP' 컬럼

                crawledRanks.add(teamData);
            } catch (Exception e) { // NumberFormatException 외에 다른 잠재적 예외도 포괄
                System.err.println("Error processing crawled row: " + row.text() + " - " + e.getMessage());
                // 오류가 발생해도 다음 행 처리를 위해 건너뜁니다.
            }
        }
        return crawledRanks;
    }
}