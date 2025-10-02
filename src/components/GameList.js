import React, { useEffect, useReducer, useCallback, useMemo } from 'react';
import { gameScheduleApi, teamApi } from '../services/apiService';
import { AuthContext } from '../context/AuthContext';
import GameCard from './GameCard';

// Initial state definition for the reducer
const initialState = {
    previousGame: null, // Previous game
    todayGame: null,    // Today's game
    nextGame: null,     // Next game
    calendarGamesByDate: {}, // Games grouped by date for calendar view
    loading: true,
    error: null,
    allTeams: [], // All team info for logo URLs
    selectedFilterYear: new Date().getFullYear(), // Default filter year
    selectedFilterMonth: new Date().getMonth() + 1, // Default filter month (1-12)
};

// Reducer function definition
function gameReducer(state, action) {
    switch (action.type) {
        case 'FETCH_START':
            return { ...state, loading: true, error: null };
        case 'FETCH_SUCCESS':
            return {
                ...state,
                loading: false,
                previousGame: action.payload.previousGame,
                todayGame: action.payload.todayGame,
                nextGame: action.payload.nextGame,
                calendarGamesByDate: action.payload.calendarGamesByDate,
                allTeams: action.payload.allTeams,
                error: null,
            };
        case 'FETCH_ERROR':
            return { ...state, loading: false, error: action.payload };
        case 'SET_FILTER_YEAR_MONTH':
            return { ...state, selectedFilterYear: action.payload.year, selectedFilterMonth: action.payload.month };
        default:
            return state;
    }
}

function GameList() {
    const [state, dispatch] = useReducer(gameReducer, initialState);
    // Destructure state for easier access
    const { previousGame, todayGame, nextGame, calendarGamesByDate, loading, error, allTeams, selectedFilterYear, selectedFilterMonth } = state;

    const { isAuthenticated, user } = React.useContext(AuthContext);

    // Helper function to format date to 'YYYY-MM-DD' string
    const getFormattedDate = useCallback((date) => {
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }, []);

    // Function to fetch game schedule data and group it
    const fetchGamesData = useCallback(async () => {
        dispatch({ type: 'FETCH_START' });
        try {
            const teamResponse = await teamApi.getAllTeams();
            const allTeamsData = teamResponse.data;

            const gameResponse = await gameScheduleApi.getAllGames();
            let allGames = gameResponse.data;

            const now = new Date();
            // Set todayStart to the beginning of the current day for accurate comparison
            const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0);

            let foundPreviousGame = null;
            let foundTodayGame = null;
            let foundNextGame = null;

            // Sort games by date to easily find previous, today, and next games
            const sortedGames = [...allGames].sort((a, b) => new Date(a.gameDate) - new Date(b.gameDate));

            for (const game of sortedGames) {
                const gameDate = new Date(game.gameDate);
                const gameDateOnly = new Date(gameDate.getFullYear(), gameDate.getMonth(), gameDate.getDate(), 0, 0, 0, 0);

                if (gameDateOnly.getTime() === todayStart.getTime()) {
                    if (!foundTodayGame) {
                        foundTodayGame = game;
                    }
                } else if (gameDate < now && game.status === 'FINISHED') {
                    foundPreviousGame = game;
                } else if (gameDate > now && game.status === 'SCHEDULED') {
                    if (!foundNextGame || gameDate < new Date(foundNextGame.gameDate)) {
                        foundNextGame = game;
                    }
                }
            }

            // Filter and group games for the calendar view based on selected year and month
            const calendarGamesGrouped = {};
            const filterDateStart = new Date(selectedFilterYear, selectedFilterMonth - 1, 1);
            const filterDateEnd = new Date(selectedFilterYear, selectedFilterMonth, 0, 23, 59, 59, 999); // End of the month

            allGames.forEach(game => {
                const gameDate = new Date(game.gameDate);
                if (gameDate >= filterDateStart && gameDate <= filterDateEnd) {
                    const dateKey = getFormattedDate(game.gameDate);
                    if (!calendarGamesGrouped[dateKey]) {
                        calendarGamesGrouped[dateKey] = [];
                    }
                    calendarGamesGrouped[dateKey].push(game);
                }
            });

            dispatch({
                type: 'FETCH_SUCCESS',
                payload: {
                    previousGame: foundPreviousGame,
                    todayGame: foundTodayGame,
                    nextGame: foundNextGame,
                    calendarGamesByDate: calendarGamesGrouped,
                    allTeams: allTeamsData,
                }
            });
        } catch (err) {
            dispatch({ type: 'FETCH_ERROR', payload: '경기 일정을 불러오는 데 실패했습니다.' });
            console.error('Error fetching games:', err);
        }
    }, [selectedFilterYear, selectedFilterMonth, getFormattedDate]);

    // ⭐ 새로운 크롤링 함수 추가 ⭐
    const handleCrawlSchedule = useCallback(async () => {
        if (!window.confirm(`${selectedFilterYear}년 ${selectedFilterMonth}월의 경기 일정을 새로 크롤링하고 업데이트하시겠습니까?`)) {
            return;
        }

        dispatch({ type: 'FETCH_START' }); // 로딩 상태 시작
        try {
            await gameScheduleApi.crawlGameSchedules(selectedFilterYear, selectedFilterMonth);
            alert('경기 일정 크롤링 및 업데이트가 완료되었습니다!');
            fetchGamesData(); // 크롤링 완료 후 최신 데이터 다시 불러오기
        } catch (err) {
            console.error('Failed to crawl and update game schedules:', err);
            dispatch({ type: 'FETCH_ERROR', payload: '경기 일정 크롤링에 실패했습니다.' });
            alert('경기 일정 크롤링 중 오류가 발생했습니다.');
        }
    }, [selectedFilterYear, selectedFilterMonth, fetchGamesData]);


    // Effect to fetch data when component mounts or filter changes
    useEffect(() => {
        fetchGamesData();
    }, [fetchGamesData]);

    // Update selected filter year and month
    const handleYearChange = (e) => {
        dispatch({ type: 'SET_FILTER_YEAR_MONTH', payload: { year: parseInt(e.target.value), month: selectedFilterMonth } });
    };

    const handleMonthChange = (e) => {
        dispatch({ type: 'SET_FILTER_YEAR_MONTH', payload: { year: selectedFilterYear, month: parseInt(e.target.value) } });
    };

    // Calendar grid generation
    const getCalendarDays = () => {
        const firstDayOfMonth = new Date(selectedFilterYear, selectedFilterMonth - 1, 1);
        const lastDayOfMonth = new Date(selectedFilterYear, selectedFilterMonth, 0);
        const numDaysInMonth = lastDayOfMonth.getDate();

        const startDayOfWeek = firstDayOfMonth.getDay(); // 0 for Sunday, 1 for Monday, ..., 6 for Saturday

        const days = [];
        // Add empty cells for days before the 1st of the month
        for (let i = 0; i < startDayOfWeek; i++) {
            days.push({ type: 'empty' });
        }

        // Add days of the current month
        for (let i = 1; i <= numDaysInMonth; i++) {
            const date = new Date(selectedFilterYear, selectedFilterMonth - 1, i);
            const formattedDate = getFormattedDate(date);
            days.push({
                type: 'day',
                date: date,
                formattedDate: formattedDate,
                games: calendarGamesByDate[formattedDate] || [],
            });
        }

        // Add empty cells to complete the last row of the calendar
        const totalCells = days.length;
        const remainingCells = 7 - (totalCells % 7);
        if (remainingCells < 7) { // Only add if not already a full row
            for (let i = 0; i < remainingCells; i++) {
                days.push({ type: 'empty' });
            }
        }

        return days;
    };

    const calendarDays = getCalendarDays();
    const dayNames = ['일', '월', '화', '수', '목', '금', '토'];

    // Generate year filter options (e.g., current year +- 5 years)
    const yearOptions = useMemo(() => {
        const years = [];
        const currentY = new Date().getFullYear();
        for (let i = currentY - 5; i <= currentY + 5; i++) {
            years.push(i);
        }
        return years;
    }, []);

    // Display loading or error messages
    if (loading) return <div className="text-center py-4 text-secondary">데이터를 불러오는 중...</div>;
    if (error) return <div className="text-center py-4 text-danger fw-bold">오류: {error}</div>;

    return (
        <div className="container my-5 p-4 bg-white rounded-lg shadow-lg">
            {/* Top monthly navigation and stats - now static title */}
            <div className="d-flex justify-content-center align-items-center mb-4 pb-3 border-bottom border-secondary-subtle">
                <div className="text-center">
                    <h3 className="mb-1 text-primary">경기 일정 및 결과</h3> {/* Static title */}
                </div>
            </div>

            {/* Previous Game, Today's Game, Next Game Section */}
            <div className="row row-cols-1 row-cols-md-3 g-3 mb-5 text-center">
                <div className="col">
                    <h4 className="text-dark mb-3">이전 경기</h4>
                    <GameCard game={previousGame} allTeams={allTeams} type="main" />
                </div>
                <div className="col">
                    <h4 className="text-danger mb-3">오늘 경기</h4> {/* Today's game highlighted in red */}
                    <GameCard game={todayGame} allTeams={allTeams} type="main" />
                </div>
                <div className="col">
                    <h4 className="text-dark mb-3">다음 경기</h4>
                    <GameCard game={nextGame} allTeams={allTeams} type="main" />
                </div>
            </div>

            <hr className="my-5 border-2 border-info" />

            {/* Other Game Schedules - Calendar View */}
            {/* ⭐ MODIFICATION START ⭐ */}
            {/* Changed 'text-start' to 'text-center' to center the date */}
            <h3 className="text-center text-dark mb-3">
                {selectedFilterYear}.{String(selectedFilterMonth).padStart(2, '0')}
            </h3>
            {/* ⭐ MODIFICATION END ⭐ */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                {/* ⭐ 관리자용 크롤링 버튼 ⭐ */}
                {isAuthenticated && user && user.role === 'ADMIN' && (
                    <button
                        className="btn btn-outline-primary btn-sm rounded-pill px-3 fw-bold shadow-sm"
                        onClick={handleCrawlSchedule}
                        disabled={loading} // 데이터 로딩 중이거나 크롤링 중일 때 비활성화
                    >
                        <i className="bi bi-arrow-clockwise me-1"></i>
                        {loading ? '크롤링 중...' : '경기 일정 크롤링'}
                    </button>
                )}
                
                {/* 연도/월 필터 */}
                <div className="d-flex gap-2">
                    <select
                        className="form-select form-select-sm"
                        value={selectedFilterYear}
                        onChange={handleYearChange}
                    >
                        {yearOptions.map(year => (
                            <option key={year} value={year}>{year}년</option>
                        ))}
                    </select>
                    <select
                        className="form-select form-select-sm"
                        value={selectedFilterMonth}
                        onChange={handleMonthChange}
                    >
                        {Array.from({ length: 12 }, (_, i) => i + 1).map(month => (
                            <option key={month} value={month}>{month}월</option>
                        ))}
                    </select>
                </div>
            </div>

            {Object.keys(calendarGamesByDate).length === 0 && !loading ? (
                <div className="alert alert-info text-center my-4" role="alert">선택된 월에 예정된 경기가 없습니다.</div>
            ) : (
                <div className="calendar-grid border border-light rounded-lg bg-light p-3">
                    {/* Day Headers - using d-flex and width: calc(100%/7) */}
                    <div className="d-flex text-center fw-bold mb-0 border-bottom">
                        {dayNames.map(day => (
                            <div key={day} className={`flex-fill py-2 ${day === '일' ? 'text-danger' : day === '토' ? 'text-primary' : 'text-dark'}`} style={{ width: 'calc(100% / 7)' }}>
                                {day}
                            </div>
                        ))}
                    </div>
                    {/* Date Cells - using d-flex flex-wrap and width: calc(100%/7) */}
                    <div className="d-flex flex-wrap">
                        {calendarDays.map((day, index) => (
                            <div key={index} className="border rounded-0 p-1 d-flex flex-column" style={{ minHeight: '200px', backgroundColor: day.type === 'empty' ? '#f8f9fa' : '#ffffff', width: 'calc(100% / 7)' }}>
                                {day.type === 'day' ? (
                                    <>
                                        {/* Display Date Number */}
                                        <div className={`text-end fw-bold mb-1 ${day.date.getDay() === 0 ? 'text-danger' : day.date.getDay() === 6 ? 'text-primary' : 'text-dark'}`}>
                                            {day.date.getDate()}
                                        </div>
                                        {/* Game Info or "No Game" */}
                                        {day.games.length > 0 ? (
                                            <div className="d-flex flex-column flex-grow-1 justify-content-start align-items-center">
                                                {day.games.map(game => (
                                                    <div key={game.id} className="small w-100 mb-1">
                                                        <GameCard game={game} allTeams={allTeams} type="calendar" />
                                                    </div>
                                                ))}
                                            </div>
                                        ) : (
                                            <div className="d-flex flex-column flex-grow-1 justify-content-center align-items-center">
                                                <GameCard game={null} allTeams={allTeams} type="calendar" />
                                            </div>
                                        )}
                                    </>
                                ) : (
                                    <div className="d-flex flex-column flex-grow-1 justify-content-center align-items-center">
                                        <GameCard game={null} allTeams={allTeams} type="calendar" />
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}

export default GameList;