import React, { useEffect, useReducer, useCallback } from 'react';
import { gameScheduleApi, teamApi } from '../services/apiService';
import TeamRankingList from './TeamRankingList';
import GameCard from './GameCard'; // GameCard 컴포넌트 임포트

// Initial state definition
const initialState = {
    previousGame: null,
    todayGame: null,
    nextGame: null,
    allTeams: [], // All team information (for logo URL mapping)
    loading: true,
    error: null,
};

// Reducer function definition
function homeReducer(state, action) {
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
                allTeams: action.payload.allTeams,
                error: null,
            };
        case 'FETCH_ERROR':
            return { ...state, loading: false, error: action.payload };
        default:
            return state;
    }
}

function Home() {
    const [state, dispatch] = useReducer(homeReducer, initialState);
    const { previousGame, todayGame, nextGame, allTeams, loading, error } = state;

    // Fetch game schedule data (previous, today, next game)
    const fetchGameOverview = useCallback(async () => {
        dispatch({ type: 'FETCH_START' });
        try {
            const teamResponse = await teamApi.getAllTeams();
            const allTeamsData = teamResponse.data;

            const gameResponse = await gameScheduleApi.getAllGames();
            let allGames = gameResponse.data;

            const now = new Date();
            const today = new Date();
            today.setHours(0, 0, 0, 0); // Normalize today to start of day

            let previousGame = null;
            let todayGame = null;
            let nextGame = null;

            // Sort games by date
            const sortedGames = allGames.sort((a, b) => new Date(a.gameDate) - new Date(b.gameDate));

            for (const game of sortedGames) {
                const gameDate = new Date(game.gameDate);
                const gameDateOnly = new Date(gameDate);
                gameDateOnly.setHours(0, 0, 0, 0);

                if (gameDateOnly.getTime() === today.getTime()) {
                    // This is a game for today
                    if (!todayGame) { // If multiple games today, just take the first one found or refine logic
                        todayGame = game;
                    }
                } else if (gameDate < now && game.status === 'FINISHED') {
                    // This is a past finished game
                    previousGame = game; // Keep updating to get the latest finished game
                } else if (gameDate > now && game.status === 'SCHEDULED') {
                    // This is a future scheduled game
                    if (!nextGame || gameDate < new Date(nextGame.gameDate)) { // Take the earliest future scheduled game
                        nextGame = game;
                    }
                }
            }

            dispatch({
                type: 'FETCH_SUCCESS',
                payload: {
                    previousGame: previousGame,
                    todayGame: todayGame,
                    nextGame: nextGame,
                    allTeams: allTeamsData,
                }
            });
        } catch (err) {
            dispatch({ type: 'FETCH_ERROR', payload: '경기 개요를 불러오는 데 실패했습니다.' });
            console.error('Error fetching game overview:', err);
        }
    }, []);

    useEffect(() => {
        fetchGameOverview();
    }, [fetchGameOverview]);

    if (loading || allTeams.length === 0) return <div className="text-center py-4 text-secondary">데이터를 불러오는 중...</div>;
    if (error) return <div className="text-center py-4 text-danger fw-bold">오류: {error}</div>;

    return (
        // Remove background image property from the entire container and set background color to transparent
        <div
            // FIX: Change my-5 to mt-4 mb-5 to adjust top margin of the entire Home component
            className="container mt-4 mb-5 p-4 rounded-lg shadow-lg"
            style={{
                backgroundColor: 'transparent',
            }}
        >
            {/* Top image section - adjust minHeight to show full width and apply objectFit: 'cover' */}
            <div
                style={{
                    minHeight: '200px', // FIX: Image height set to 200px (half of 400px)
                    borderRadius: '8px',
                    // FIX: Increased margin between image section and cards below (0.25rem -> 0.5rem)
                    marginBottom: '1.5rem',
                    backgroundColor: 'transparent',
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    overflow: 'hidden', // Hide overflow to prevent image from going outside the section
                }}
                className="rounded-lg"
            >
                {/* Added image */}
                <img
                    src="homeimg.jpg" // FIX: Changed image source to homeimg.jpg
                    alt="롯데 자이언츠 선수 이미지"
                    style={{
                        width: '100%', // Fill full width
                        height: '100%', // Fill full height
                        objectFit: 'cover', // Fill the section while maintaining aspect ratio
                        display: 'block',
                        userSelect: 'none',
                        margin: 'auto',
                        transition: 'background-color 300ms',
                    }}
                    onError={(e) => { e.target.onerror = null; e.target.src = 'https://placehold.co/400x200/CCCCCC/000000?text=Image+Error'; }} // FIX: Adjust placeholder image height on error
                />
            </div>

            {/* Team ranking and game schedule card section */}
            {/* FIX: gap 속성을 제거하여 카드들이 한 줄에 나란히 표시되도록 수정 */}
            <div className="row justify-content-center">
                {/* Left Column: 팀 순위 */}
                {/* FIX: col-md-5 유지 */}
                <div className="col-md-5 mx-auto">
                    <div className="card shadow-lg mb-4 border-0 rounded-lg">
                        <div className="card-header bg-primary text-white fw-bold rounded-top py-2" style={{ backgroundColor: '#1E3965' }}>
                            <h5 className="mb-0">팀 순위</h5>
                        </div>
                        {/* Remove maxHeight and overflowY from team ranking section (to show all without scrollbar) */}
                        <div className="card-body p-0">
                            <TeamRankingList />
                        </div>
                    </div>
                </div>

                {/* Right Column: 경기 일정 */}
                {/* FIX: col-md-7 유지 */}
                <div className="col-md-7 mx-auto">
                    <div className="card shadow-lg mb-4 border-0 rounded-lg">
                        <div className="card-header bg-danger text-white fw-bold rounded-top py-2" style={{ backgroundColor: '#CD112B' }}>
                            <h5 className="mb-0">경기 일정</h5>
                        </div>
                        <div className="card-body d-flex flex-column gap-3 px-3 pb-3 pt-0" style={{ minHeight: '350px' }}> {/* Adjust minHeight */}
                            <div>
                                <h5 className="text-dark mb-2">이전 경기</h5>
                                <GameCard game={previousGame} allTeams={allTeams} type="list" />
                            </div>
                            <div>
                                <h5 className="text-danger mb-2">오늘 경기</h5>
                                <GameCard game={todayGame} allTeams={allTeams} type="list" />
                            </div>
                            <div>
                                <h5 className="text-dark mb-2">다음 경기</h5>
                                <GameCard game={nextGame} allTeams={allTeams} type="list" />
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Home;
