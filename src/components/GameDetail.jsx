import React, { useEffect, useContext, useCallback, useReducer } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { gameScheduleApi, lineupApi, teamApi } from '../services/apiService';
import { AuthContext } from '../context/AuthContext';
import Modal from './Modal';
import CommentSection from './CommentSection';
import LineupSection from './LineupSection'; // LineupSection import

// --- Helper Functions ---
// These helper functions are exported so LineupSection can also import them if needed.
export const getTeamLogoUrl = (teamId, allTeams) => {
    const team = allTeams.find(t => t.id === teamId);
    return team && team.logoUrl ? team.logoUrl : 'https://placehold.co/100x100/CCCCCC/000000?text=Logo';
};

export const handleImageError = (e) => {
    e.target.onerror = null;
    e.target.src = 'https://placehold.co/80x80/CCCCCC/000000?text=Logo';
};

// --- Refactored State and Reducer ---

const initialState = {
    // Game fetch states
    game: null,
    loading: true,
    error: null,

    // Consolidated useState variables
    modalMessage: '',
    showConfirmModal: false,
    isEditMode: false,
    editedGame: null,
    allAvailableTeams: [],
    homeExpectedLineup: null,
    awayExpectedLineup: null,
    isHomeLineupEditMode: false,
    isAwayLineupEditMode: false,
    editedHomeLineupPlayers: [],
    editedAwayLineupPlayers: [],
    isCrawling: false, // State to track if crawling is in progress
};

function gameDetailReducer(state, action) {
    switch (action.type) {
        // Game Fetch Actions
        case 'FETCH_START':
            return { ...state, loading: true, error: null };
        case 'FETCH_SUCCESS':
            return { ...state, loading: false, game: action.payload, editedGame: action.payload };
        case 'FETCH_ERROR':
            return { ...state, loading: false, error: action.payload };

        // General Game & Team Management
        case 'UPDATE_GAME_SUCCESS':
            return { ...state, game: action.payload, editedGame: action.payload };
        case 'SET_EDIT_MODE':
            return { ...state, isEditMode: action.payload };
        case 'SET_EDITED_GAME':
            return { ...state, editedGame: action.payload };
        case 'SET_ALL_TEAMS':
            return { ...state, allAvailableTeams: action.payload };
        
        // Modal Management
        case 'SET_MODAL_MESSAGE':
            return { ...state, modalMessage: action.payload };
        case 'SET_SHOW_CONFIRM_MODAL':
            return { ...state, showConfirmModal: action.payload };
        
        // Lineup Management
        case 'SET_HOME_LINEUP_EDIT_MODE':
            return { ...state, isHomeLineupEditMode: action.payload };
        case 'SET_AWAY_LINEUP_EDIT_MODE':
            return { ...state, isAwayLineupEditMode: action.payload };
        case 'SET_EXPECTED_LINEUP_HOME':
            return { ...state, homeExpectedLineup: action.payload };
        case 'SET_EXPECTED_LINEUP_AWAY':
            return { ...state, awayExpectedLineup: action.payload };
        case 'SET_EDITED_HOME_PLAYERS':
            return { ...state, editedHomeLineupPlayers: action.payload };
        case 'SET_EDITED_AWAY_PLAYERS':
            return { ...state, editedAwayLineupPlayers: action.payload };
        case 'SET_CRAWLING_STATUS': // Reducer action for crawling status
            return { ...state, isCrawling: action.payload };
        
        default:
            return state;
    }
}

// --- GameDetail Component ---

function GameDetail() {
    const { gameId } = useParams();
    const navigate = useNavigate();
    const [state, dispatch] = useReducer(gameDetailReducer, initialState);
    const { user, loading: authLoading } = useContext(AuthContext);

    // Destructure all states from the reducer's state object
    const {
        game,
        loading,
        error,
        modalMessage,
        showConfirmModal,
        isEditMode,
        editedGame,
        allAvailableTeams,
        homeExpectedLineup,
        awayExpectedLineup,
        isHomeLineupEditMode,
        isAwayLineupEditMode,
        editedHomeLineupPlayers,
        editedAwayLineupPlayers,
        isCrawling, // Destructure isCrawling state
    } = state;

    // Derived states
    const currentUser = user ? user.username : null;
    const isAdmin = user ? (user.role === 'ADMIN' || user.role === 'ROLE_ADMIN') : false;

    // --- Data Fetching and Refresh Function ---
    
    // Function to fetch both game and lineup data. Memoized via useCallback.
    const refreshGameAndLineupData = useCallback(async () => {
        dispatch({ type: 'FETCH_START' });
        try {
            const gameResponse = await gameScheduleApi.getGameById(gameId);
            dispatch({ type: 'FETCH_SUCCESS', payload: gameResponse.data });

            // Fetch lineup data
            try {
                const lineupResponse = await lineupApi.getLineupByGameId(gameId);
                const homeLineupData = lineupResponse.data.homeLineup;
                const awayLineupData = lineupResponse.data.awayLineup;

                // Helper to sort players by orderNumber, handling nulls
                const sortPlayers = (lineup) => 
                    lineup && lineup.players 
                    ? [...lineup.players].sort((a, b) => {
                        // null orderNumber를 가장 뒤로 보내기
                        const orderA = a.orderNumber === null ? Number.MAX_SAFE_INTEGER : a.orderNumber;
                        const orderB = b.orderNumber === null ? Number.MAX_SAFE_INTEGER : b.orderNumber;
                        return orderA - orderB;
                    }) 
                    : [];

                if (homeLineupData) {
                    const sortedHomePlayers = sortPlayers(homeLineupData);
                    dispatch({ type: 'SET_EXPECTED_LINEUP_HOME', payload: { ...homeLineupData, players: sortedHomePlayers } });
                    dispatch({ type: 'SET_EDITED_HOME_PLAYERS', payload: sortedHomePlayers });
                } else {
                    dispatch({ type: 'SET_EXPECTED_LINEUP_HOME', payload: null });
                    dispatch({ type: 'SET_EDITED_HOME_PLAYERS', payload: [] });
                }

                if (awayLineupData) {
                    const sortedAwayPlayers = sortPlayers(awayLineupData);
                    dispatch({ type: 'SET_EXPECTED_LINEUP_AWAY', payload: { ...awayLineupData, players: sortedAwayPlayers } });
                    dispatch({ type: 'SET_EDITED_AWAY_PLAYERS', payload: sortedAwayPlayers });
                } else {
                    dispatch({ type: 'SET_EXPECTED_LINEUP_AWAY', payload: null });
                    dispatch({ type: 'SET_EDITED_AWAY_PLAYERS', payload: [] });
                }
            } catch (lineupError) {
                if (lineupError.response && lineupError.response.status === 404) {
                    // Handle 404 for lineups specifically (no lineup exists yet)
                    dispatch({ type: 'SET_EXPECTED_LINEUP_HOME', payload: null });
                    dispatch({ type: 'SET_EDITED_HOME_PLAYERS', payload: [] });
                    dispatch({ type: 'SET_EXPECTED_LINEUP_AWAY', payload: null });
                    dispatch({ type: 'SET_EDITED_AWAY_PLAYERS', payload: [] });
                } else {
                    console.error('라인업 상세 정보를 불러오는 데 실패했습니다:', lineupError);
                    dispatch({ type: 'SET_MODAL_MESSAGE', payload: '라인업 정보를 불러오는 데 실패했습니다.' });
                }
            }
        } catch (err) {
            dispatch({ type: 'FETCH_ERROR', payload: '경기 상세 정보를 불러오는 데 실패했습니다.' });
            console.error('경기 상세 정보를 불러오는 데 실패했습니다:', err);
        }
    }, [gameId, dispatch]); // Dependencies for useCallback

    // --- useEffect Hooks ---

    // Fetch teams on component mount
    useEffect(() => {
        const fetchTeams = async () => {
            try {
                const response = await teamApi.getAllTeams();
                dispatch({ type: 'SET_ALL_TEAMS', payload: response.data });
            } catch (error) {
                console.error('Failed to fetch teams:', error);
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: '팀 목록을 불러오는 데 실패했습니다.' });
            }
        };
        fetchTeams();
    }, []);

    // Fetch game and lineup data when gameId or allAvailableTeams changes, using the refresh function
    useEffect(() => {
        if (gameId && allAvailableTeams.length > 0) {
            refreshGameAndLineupData();
        }
    }, [gameId, allAvailableTeams, refreshGameAndLineupData]);

    // --- Lineup Crawling Functionality ---

    // Function to handle the lineup scraping request
    const handleCrawlLineup = useCallback(async () => {
        if (!isAdmin) {
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '크롤링 권한이 없습니다.' });
            return;
        }

        dispatch({ type: 'SET_MODAL_MESSAGE', payload: '라인업 크롤링을 시작합니다. 이 작업은 다소 시간이 걸릴 수 있습니다...' });
        dispatch({ type: 'SET_CRAWLING_STATUS', payload: true });

        try {
            // Call the backend API for crawling
            await lineupApi.crawlLineups(gameId); 

            // After successful crawl, refresh the data to display the newly scraped lineup.
            await refreshGameAndLineupData();

            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '라인업 크롤링 및 저장이 완료되었습니다. 데이터가 업데이트되었습니다.' });
        } catch (error) {
            console.error('Lineup crawl failed:', error);
            const errorMessage = error.response?.data?.message || error.message || '알 수 없는 오류가 발생했습니다.';
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: `라인업 크롤링에 실패했습니다: ${errorMessage}` });
        } finally {
            dispatch({ type: 'SET_CRAWLING_STATUS', payload: false });
        }
    }, [gameId, isAdmin, dispatch, refreshGameAndLineupData]); // Dependencies for useCallback

    // --- Event Handlers (Unchanged from original) ---

    const handleFieldChange = (e) => {
        const { name, value } = e.target;
        dispatch({
            type: 'SET_EDITED_GAME',
            payload: {
                ...editedGame,
                [name]: value
            }
        });
    };

    const handleTeamChange = (e, teamType) => {
        const selectedTeamId = parseInt(e.target.value, 10);
        const selectedTeam = allAvailableTeams.find(team => team.id === selectedTeamId);

        let updatedEditedGame;
        if (selectedTeam) {
            updatedEditedGame = {
                ...editedGame,
                [teamType]: {
                    id: selectedTeam.id,
                    name: selectedTeam.name,
                    logoUrl: selectedTeam.logoUrl
                }
            };
        } else {
            updatedEditedGame = {
                ...editedGame,
                [teamType]: null
            };
        }
        dispatch({ type: 'SET_EDITED_GAME', payload: updatedEditedGame });
    };

    // Helper function to update lineup players (used for both home and away)
    const updateLineupPlayers = (players, index, field, value) => {
        const newPlayers = [...players];
        newPlayers[index] = {
            ...newPlayers[index],
            [field]: field === 'orderNumber' ? parseInt(value, 10) : value
        };
        return newPlayers;
    };

    const handleLineupPlayerChange = (teamType, index, field, value) => {
        if (teamType === 'home') {
            const updatedPlayers = updateLineupPlayers(editedHomeLineupPlayers, index, field, value);
            dispatch({ type: 'SET_EDITED_HOME_PLAYERS', payload: updatedPlayers });
        } else if (teamType === 'away') {
            const updatedPlayers = updateLineupPlayers(editedAwayLineupPlayers, index, field, value);
            dispatch({ type: 'SET_EDITED_AWAY_PLAYERS', payload: updatedPlayers });
        }
    };

    const handleAddPlayer = (teamType) => {
        const newPlayer = { orderNumber: 0, playerName: '', position: '' };
        if (teamType === 'home') {
            const updatedPlayers = [...editedHomeLineupPlayers, { ...newPlayer, orderNumber: editedHomeLineupPlayers.length + 1 }];
            dispatch({ type: 'SET_EDITED_HOME_PLAYERS', payload: updatedPlayers });
        } else if (teamType === 'away') {
            const updatedPlayers = [...editedAwayLineupPlayers, { ...newPlayer, orderNumber: editedAwayLineupPlayers.length + 1 }];
            dispatch({ type: 'SET_EDITED_AWAY_PLAYERS', payload: updatedPlayers });
        }
    };

    const handleRemovePlayer = (teamType, indexToRemove) => {
        const filterAndReorder = (players) => {
            return players
                .filter((_, index) => index !== indexToRemove)
                .map((player, idx) => ({ ...player, orderNumber: idx + 1 }));
        };

        if (teamType === 'home') {
            const updatedPlayers = filterAndReorder(editedHomeLineupPlayers);
            dispatch({ type: 'SET_EDITED_HOME_PLAYERS', payload: updatedPlayers });
        } else if (teamType === 'away') {
            const updatedPlayers = filterAndReorder(editedAwayLineupPlayers);
            dispatch({ type: 'SET_EDITED_AWAY_PLAYERS', payload: updatedPlayers });
        }
    };

    const handleUpdateSubmit = async () => {
        if (!editedGame || !editedGame.homeTeam || !editedGame.opponentTeam) {
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '홈 팀과 원정 팀을 모두 선택해주세요.' });
            return;
        }

        try {
            // Ensure scores are numbers
            const updatedData = {
                ...editedGame,
                homeScore: editedGame.homeScore !== undefined ? parseInt(editedGame.homeScore, 10) : null,
                awayScore: editedGame.awayScore !== undefined ? parseInt(editedGame.awayScore, 10) : null,
            };

            const response = await gameScheduleApi.updateGame(gameId, updatedData);
            dispatch({ type: 'UPDATE_GAME_SUCCESS', payload: response.data });
            dispatch({ type: 'SET_EDIT_MODE', payload: false });
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '경기가 성공적으로 수정되었습니다!' });
        } catch (error) {
            console.error('경기 수정 실패:', error);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: `경기 수정에 실패했습니다: ${error.response?.data?.message || error.message}` });
        }
    };

    const handleLineupUpdateSubmit = async (teamType) => {
        try {
            const playersToUpdate = teamType === 'home' ? editedHomeLineupPlayers : editedAwayLineupPlayers;
            const validPlayers = playersToUpdate.filter(player => player.playerName && player.position);
            
            // Sorting players with null-safe orderNumber comparison
            const sortedPlayers = [...validPlayers].sort((a, b) => {
                const orderA = a.orderNumber === null ? Number.MAX_SAFE_INTEGER : a.orderNumber;
                const orderB = b.orderNumber === null ? Number.MAX_SAFE_INTEGER : b.orderNumber;
                return orderA - orderB;
            });

            const lineupDTO = {
                gameId: gameId,
                teamType: teamType.toUpperCase(),
                players: sortedPlayers
            };

            const response = await lineupApi.createOrUpdateLineup(gameId, lineupDTO);
            
            // Update state with the confirmed lineup data from the backend
            if (teamType === 'home') {
                dispatch({ type: 'SET_EXPECTED_LINEUP_HOME', payload: response.data });
                dispatch({ type: 'SET_EDITED_HOME_PLAYERS', payload: response.data.players.sort((a, b) => {
                    const orderA = a.orderNumber === null ? Number.MAX_SAFE_INTEGER : a.orderNumber;
                    const orderB = b.orderNumber === null ? Number.MAX_SAFE_INTEGER : b.orderB;
                    return orderA - orderB;
                }) });
                dispatch({ type: 'SET_HOME_LINEUP_EDIT_MODE', payload: false });
            } else {
                dispatch({ type: 'SET_EXPECTED_LINEUP_AWAY', payload: response.data });
                dispatch({ type: 'SET_EDITED_AWAY_PLAYERS', payload: response.data.players.sort((a, b) => {
                    const orderA = a.orderNumber === null ? Number.MAX_SAFE_INTEGER : a.orderNumber;
                    const orderB = b.orderNumber === null ? Number.MAX_SAFE_INTEGER : b.orderB;
                    return orderA - orderB;
                }) });
                dispatch({ type: 'SET_AWAY_LINEUP_EDIT_MODE', payload: false });
            }
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: `예상 ${teamType === 'home' ? '홈 팀' : '원정 팀'} 라인업이 성공적으로 업데이트되었습니다!` });
        } catch (error) {
            console.error(`${teamType === 'home' ? '홈 팀' : '원정 팀'} 라인업 수정 실패:`, error);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: `${teamType === 'home' ? '홈 팀' : '원정 팀'} 라인업 수정에 실패했습니다.` });
        }
    };

    const handleDeleteGame = () => {
        dispatch({ type: 'SET_MODAL_MESSAGE', payload: '정말로 이 경기를 삭제하시겠습니까?' });
        dispatch({ type: 'SET_SHOW_CONFIRM_MODAL', payload: true });
    };

    const confirmDeleteGame = async () => {
        dispatch({ type: 'SET_SHOW_CONFIRM_MODAL', payload: false });
        try {
            await gameScheduleApi.deleteGame(gameId);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '경기가 성공적으로 삭제되었습니다.' });
            navigate('/');
        } catch (error) {
            console.error('경기 삭제 실패:', error);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '경기 삭제에 실패했습니다.' });
        }
    };

    const handleModalClose = () => {
        dispatch({ type: 'SET_MODAL_MESSAGE', payload: '' });
        dispatch({ type: 'SET_SHOW_CONFIRM_MODAL', payload: false });
    };

    // --- Utility Functions (Unchanged) ---

    const formatGameDate = (dateString) => {
        if (!dateString) return '';
        const parts = dateString.split('T');
        if (parts.length < 2) return dateString;
        const datePart = parts[0];
        const timePart = parts[1].substring(0, 5);
        return `${datePart} ${timePart}`;
    };

    const formatDateTimeLocal = (dateString) => {
        if (!dateString) return '';
        return dateString.substring(0, 16);
    };

    const getKoreanStatus = (status) => {
        switch (status) {
            case 'SCHEDULED': return '예정';
            case 'IN_PROGRESS': return '진행 중';
            case 'FINISHED': return '종료';
            case 'CANCELED': return '취소';
            default: return status;
        }
    };

    const getWinLossStatus = useCallback((game) => {
        if (!game || game.status !== 'FINISHED' || !game.homeTeam || !game.opponentTeam) {
            return null;
        }

        if (game.homeScore > game.awayScore) {
            return {
                home: '승',
                opponent: '패'
            };
        } else if (game.homeScore < game.awayScore) {
            return {
                home: '패',
                opponent: '승'
            };
        } else {
            return {
                home: '무',
                opponent: '무'
            };
        }
    }, []); // No dependencies needed if 'game' is always the latest state

    // --- Component Rendering ---

    if (loading || authLoading) return <div className="text-center py-4 text-secondary">경기 상세 정보를 불러오는 중...</div>;
    if (error) return <div className="text-center py-4 text-danger fw-bold">오류: {error}</div>;
    if (!game || !editedGame) return <div className="text-center py-4 text-secondary">경기를 찾을 수 없습니다.</div>;

    const homeTeamInfo = game.homeTeam;
    const opponentTeamInfo = game.opponentTeam;

    const homeTeamLogo = homeTeamInfo ? getTeamLogoUrl(homeTeamInfo.id, allAvailableTeams) : 'https://placehold.co/50x50/CCCCCC/000000?text=Logo';
    const opponentTeamLogo = opponentTeamInfo ? getTeamLogoUrl(opponentTeamInfo.id, allAvailableTeams) : 'https://placehold.co/50x50/CCCCCC/000000?text=Logo';

    const winLoss = getWinLossStatus(game);

    return (
        <div className="container my-5 p-4 bg-white rounded-lg shadow-lg text-center">
            {/* 뒤로가기 버튼과 제목 */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <button 
                    className="btn btn-outline-secondary rounded-pill px-4 py-2 fw-bold"
                    onClick={() => navigate(-1)} 
                >
                    &larr; 경기일정 목록으로
                </button>
                <h2 className="text-center text-dark mb-0 pb-2 border-bottom border-transparent">경기 상세 정보</h2>
                <div style={{ width: '150px' }}></div> 
            </div>

            <div className="fs-5 text-secondary mb-4">
                <p className="mb-2">
                    <strong className="text-dark">경기 날짜:</strong>
                    {isEditMode ? (
                        <input
                            type="datetime-local"
                            name="gameDate"
                            value={formatDateTimeLocal(editedGame.gameDate)}
                            onChange={handleFieldChange}
                            className="form-control d-inline-block w-auto ms-2"
                        />
                    ) : (
                        <span> {formatGameDate(game.gameDate)}</span>
                    )}
                </p>
                <p className="mb-2">
                    <strong className="text-dark">홈 팀:</strong>
                    {isEditMode ? (
                        <select
                            name="homeTeam"
                            value={editedGame.homeTeam ? editedGame.homeTeam.id : ''}
                            onChange={(e) => handleTeamChange(e, 'homeTeam')}
                            className="form-select d-inline-block w-auto ms-2"
                        >
                            <option value="">홈 팀을 선택하세요</option>
                            {allAvailableTeams.map((team) => (
                                <option key={team.id} value={team.id}>{team.name}</option>
                            ))}
                        </select>
                    ) : (
                        <span> {game.homeTeam ? game.homeTeam.name : '정보 없음'}</span>
                    )}
                </p>
                <p className="mb-2">
                    <strong className="text-dark">원정 팀:</strong>
                    {isEditMode ? (
                        <select
                            name="opponentTeam"
                            value={editedGame.opponentTeam ? editedGame.opponentTeam.id : ''}
                            onChange={(e) => handleTeamChange(e, 'opponentTeam')}
                            className="form-select d-inline-block w-auto ms-2"
                        >
                            <option value="">원정 팀을 선택하세요</option>
                            {allAvailableTeams.map((team) => (
                                <option key={team.id} value={team.id}>{team.name}</option>
                            ))}
                        </select>
                    ) : (
                        <span> {game.opponentTeam ? game.opponentTeam.name : '정보 없음'}</span>
                    )}
                </p>
                <div className="d-flex justify-content-center align-items-center my-4">
                    <div className="d-flex flex-column align-items-center mx-4">
                        <img src={homeTeamLogo} alt={`${homeTeamInfo ? homeTeamInfo.name : '홈 팀'} 로고`} style={{ height: '80px', marginBottom: '10px' }} onError={handleImageError} />
                        <span className="fw-bold fs-4 text-primary">{homeTeamInfo ? homeTeamInfo.name : '홈 팀'}</span>
                        {game.status === 'FINISHED' && winLoss && (
                            <span className={`fw-bold fs-5 ${winLoss.home === '승' ? 'text-success' : winLoss.home === '패' ? 'text-danger' : 'text-secondary'}`}>
                                {winLoss.home}
                            </span>
                        )}
                    </div>
                    <div className="d-flex align-items-center mx-4">
                        {isEditMode ? (
                            <>
                                <input
                                    type="number"
                                    name="homeScore"
                                    value={editedGame.homeScore || 0}
                                    onChange={handleFieldChange}
                                    className="form-control form-control-lg text-center mx-2"
                                    style={{ width: '80px', fontSize: '2rem' }}
                                />
                                <span className="fs-1 fw-bold text-dark mx-2">VS</span>
                                <input
                                    type="number"
                                    name="awayScore"
                                    value={editedGame.awayScore || 0}
                                    onChange={handleFieldChange}
                                    className="form-control form-control-lg text-center mx-2"
                                    style={{ width: '80px', fontSize: '2rem' }}
                                />
                            </>
                        ) : (
                            <>
                                <span className="fs-1 fw-bold text-dark">{game.homeScore}</span>
                                <span className="fs-1 fw-bold text-dark mx-4">VS</span>
                                <span className="fs-1 fw-bold text-dark">{game.awayScore}</span>
                            </>
                        )}
                    </div>
                    <div className="d-flex flex-column align-items-center mx-4">
                        <img src={opponentTeamLogo} alt={`${opponentTeamInfo ? opponentTeamInfo.name : '원정 팀'} 로고`} style={{ height: '80px', marginBottom: '10px' }} onError={handleImageError} />
                        <span className="fw-bold fs-4 text-danger">{opponentTeamInfo ? opponentTeamInfo.name : '원정 팀'}</span>
                        {game.status === 'FINISHED' && winLoss && (
                            <span className={`fw-bold fs-5 ${winLoss.opponent === '승' ? 'text-success' : winLoss.opponent === '패' ? 'text-danger' : 'text-secondary'}`}>
                                {winLoss.opponent}
                            </span>
                        )}
                    </div>
                </div>
                <p className="mb-2">
                    <strong className="text-dark">진행 상태:</strong>
                    {isEditMode ? (
                        <select
                            name="status"
                            value={editedGame.status || ''}
                            onChange={handleFieldChange}
                            className="form-select d-inline-block w-auto ms-2"
                        >
                            <option value="SCHEDULED">예정</option>
                            <option value="IN_PROGRESS">진행 중</option>
                            <option value="FINISHED">종료</option>
                            <option value="CANCELED">취소</option>
                        </select>
                    ) : (
                        <span> {getKoreanStatus(game.status)}</span>
                    )}
                </p>
                {/* Location field */}
                <p className="mb-2">
                    <strong className="text-dark">경기 위치:</strong>
                    {isEditMode ? (
                        <input
                            type="text"
                            name="location"
                            value={editedGame.location || ''}
                            onChange={handleFieldChange}
                            className="form-control d-inline-block w-auto ms-2"
                        />
                    ) : (
                        <span> {game.location || '정보 없음'}</span>
                    )}
                </p>
            </div>

            {/* Admin Controls: Save, Cancel, Edit, Delete */}
            {isAdmin && (
                <div className="d-flex justify-content-center gap-3 mt-4">
                    {isEditMode ? (
                        <>
                            <button
                                className="btn btn-success rounded-pill px-4 py-2 fw-bold shadow-sm"
                                onClick={handleUpdateSubmit}
                            >
                                저장
                            </button>
                            <button
                                className="btn btn-secondary rounded-pill px-4 py-2 fw-bold shadow-sm"
                                onClick={() => {
                                    dispatch({ type: 'SET_EDIT_MODE', payload: false });
                                    dispatch({ type: 'SET_EDITED_GAME', payload: game }); 
                                }}
                            >
                                취소
                            </button>
                        </>
                    ) : (
                        <>
                            <button
                                className="btn btn-primary rounded-pill px-4 py-2 fw-bold shadow-sm"
                                onClick={() => dispatch({ type: 'SET_EDIT_MODE', payload: true })}
                            >
                                수정
                            </button>
                            <button
                                className="btn btn-danger rounded-pill px-4 py-2 fw-bold shadow-sm"
                                onClick={handleDeleteGame}
                            >
                                삭제
                            </button>
                        </>
                    )}
                </div>
            )}

            <hr className="my-5 border-2 border-transparent" />
            
            <h3 className="text-center text-dark mb-4">예상 선발 라인업</h3>
            
            {/* 라인업 크롤링 버튼 (관리자에게만 보임) */}
            {isAdmin && (
                <div className="text-center mb-4">
                    <button
                        className="btn btn-info rounded-pill px-4 py-2 fw-bold shadow-sm"
                        onClick={handleCrawlLineup}
                        disabled={isCrawling} // Disable button while crawling
                    >
                        {isCrawling ? '라인업 크롤링 중...' : '라인업 크롤링 (Scrape Lineup)'}
                    </button>
                </div>
            )}

            <div className="row justify-content-center">
                {/* Home Lineup Section */}
                <LineupSection
                    teamType="home"
                    teamInfo={homeTeamInfo}
                    allAvailableTeams={allAvailableTeams}
                    expectedLineup={homeExpectedLineup}
                    editedLineupPlayers={editedHomeLineupPlayers}
                    isLineupEditMode={isHomeLineupEditMode}
                    isAdmin={isAdmin}
                    dispatch={dispatch}
                    handleLineupPlayerChange={handleLineupPlayerChange}
                    handleAddPlayer={handleAddPlayer}
                    handleRemovePlayer={handleRemovePlayer}
                    handleLineupUpdateSubmit={handleLineupUpdateSubmit}
                />

                {/* Away Lineup Section */}
                <LineupSection
                    teamType="away"
                    teamInfo={opponentTeamInfo}
                    allAvailableTeams={allAvailableTeams}
                    expectedLineup={awayExpectedLineup}
                    editedLineupPlayers={editedAwayLineupPlayers}
                    isLineupEditMode={isAwayLineupEditMode}
                    isAdmin={isAdmin}
                    dispatch={dispatch}
                    handleLineupPlayerChange={handleLineupPlayerChange}
                    handleAddPlayer={handleAddPlayer}
                    handleRemovePlayer={handleRemovePlayer}
                    handleLineupUpdateSubmit={handleLineupUpdateSubmit}
                />
            </div>

            {/* Comment Section */}
            <CommentSection
                gameId={gameId}
                currentUser={currentUser}
                isAdmin={isAdmin}
                homeTeamName={game.homeTeam?.name}
                opponentTeamName={game.opponentTeam?.name}
            />

            {/* Modals */}
            {modalMessage && !showConfirmModal && (
                <Modal
                    message={modalMessage}
                    onClose={handleModalClose}
                />
            )}
            
            {showConfirmModal && (
                <Modal
                    message="정말로 이 경기를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다."
                    onClose={handleModalClose}
                    onConfirm={confirmDeleteGame}
                    showConfirmButton={true}
                    title="경기 삭제 확인"
                />
            )}
        </div>
    );
}

export default GameDetail;
