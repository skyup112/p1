import React, { useEffect, useReducer, useCallback } from 'react';
import { teamRankingApi, teamApi } from '../services/apiService';
import Modal from './Modal'; // Generic modal for messages
import TeamRankingFormModal from './TeamRankingFormModal'; // New: Import the dedicated team ranking form modal

// 1. Initial State Definition for useReducer
const initialState = {
    rankings: [],
    allTeams: [], // 팀 선택 드롭다운에 사용될 모든 팀 목록
    loading: true,
    error: null,
    modalMessage: '',
    isCreateModalOpen: false,
    isEditModalOpen: false,
    currentRankingToEdit: null,
    selectedSeasonYear: new Date().getFullYear(), // 관리할 시즌 연도 선택 (UI는 제거하지만 내부적으로 사용)
};

// 2. Reducer Function Definition
function adminTeamRankingsReducer(state, action) {
    switch (action.type) {
        case 'FETCH_START':
            return { ...state, loading: true, error: null };
        case 'FETCH_SUCCESS':
            return { ...state, loading: false, rankings: action.payload.rankings, allTeams: action.payload.allTeams };
        case 'FETCH_ERROR':
            return { ...state, loading: false, error: action.payload, modalMessage: '팀 목록을 불러오는 데 실패했습니다.' };
        case 'SET_MODAL_MESSAGE':
            return { ...state, modalMessage: action.payload };
        case 'CLEAR_MODAL_MESSAGE':
            return { ...state, modalMessage: '' };
        case 'OPEN_CREATE_MODAL':
            return { ...state, isCreateModalOpen: true };
        case 'CLOSE_CREATE_MODAL':
            return { ...state, isCreateModalOpen: false };
        case 'OPEN_EDIT_MODAL':
            return { ...state, isEditModalOpen: true, currentRankingToEdit: action.payload };
        case 'CLOSE_EDIT_MODAL':
            return { ...state, isEditModalOpen: false, currentRankingToEdit: null };
        case 'ADD_RANKING_SUCCESS':
            return {
                ...state,
                rankings: [...state.rankings, action.payload].sort((a, b) => a.currentRank - b.currentRank), // 추가 후 순위로 정렬
                isCreateModalOpen: false,
                modalMessage: '팀 순위가 성공적으로 등록되었습니다.'
            };
        case 'UPDATE_RANKING_SUCCESS':
            const updatedRankings = state.rankings.map(ranking =>
                ranking.id === action.payload.id ? action.payload : ranking
            ).sort((a, b) => a.currentRank - b.currentRank); // 업데이트 후 순위로 정렬
            return {
                ...state,
                rankings: updatedRankings,
                isEditModalOpen: false,
                currentRankingToEdit: null,
                modalMessage: '팀 순위 정보가 성공적으로 수정되었습니다.'
            };
        case 'DELETE_RANKING_SUCCESS':
            const remainingRankings = state.rankings.filter(ranking => ranking.id !== action.payload);
            return {
                ...state,
                rankings: remainingRankings,
                modalMessage: '팀 순위가 성공적으로 삭제되었습니다.'
            };
        case 'SET_SEASON_YEAR': // 이 액션은 더 이상 UI에서 직접 호출되지 않지만, 상태에는 유지
            return { ...state, selectedSeasonYear: action.payload };
        case 'SET_LOADING': // 명시적으로 로딩 상태를 설정하는 액션 추가
            return { ...state, loading: action.payload };
        default:
            return state;
    }
}

function AdminTeamRankings() {
    const [state, dispatch] = useReducer(adminTeamRankingsReducer, initialState);
    const { rankings, allTeams, loading, error, modalMessage, isCreateModalOpen, isEditModalOpen, currentRankingToEdit, selectedSeasonYear } = state;

    // 팀 로고 URL 가져오기 헬퍼 함수
    const getTeamLogoUrl = useCallback((teamName) => {
        const team = allTeams.find(t => t.name === teamName);
        return team ? team.logoUrl : 'https://placehold.co/100x100/CCCCCC/000000?text=Logo';
    }, [allTeams]);

    // 이미지 로딩 실패 핸들러
    const handleImageError = useCallback((e) => {
        e.target.onerror = null; // 무한 루프 방지
        e.target.src = 'https://placehold.co/50x50/CCCCCC/000000?text=Logo'; // 대체 이미지
    }, []);

    // Function to fetch team rankings and all teams
    // useCallback으로 감싸서 함수가 재생성되는 것을 방지하고, useEffect의 의존성으로 추가합니다.
    const fetchRankingsAndTeams = useCallback(async () => {
        dispatch({ type: 'FETCH_START' });
        try {
            const teamResponse = await teamApi.getAllTeams();
            const rankingResponse = await teamRankingApi.getAllTeamRankings(selectedSeasonYear); // selectedSeasonYear 사용
            dispatch({ type: 'FETCH_SUCCESS', payload: { rankings: rankingResponse.data, allTeams: teamResponse.data } });
        } catch (err) {
            console.error('팀 순위 및 팀 목록 불러오기 실패:', err);
            dispatch({ type: 'FETCH_ERROR', payload: err.message });
        }
    }, [selectedSeasonYear, dispatch]); // dispatch는 안정적이지만, ESLint 규칙을 위해 포함

    // Fetch data on component mount and when selectedSeasonYear changes
    useEffect(() => {
        fetchRankingsAndTeams();
    }, [selectedSeasonYear, fetchRankingsAndTeams]); // fetchRankingsAndTeams를 의존성 배열에 추가

    // Handle generic modal close (for messages)
    const handleModalClose = () => {
        dispatch({ type: 'CLEAR_MODAL_MESSAGE' });
    };

    // Handle opening create ranking modal
    const handleOpenCreateModal = () => {
        dispatch({ type: 'OPEN_CREATE_MODAL' });
    };

    // Handle closing create ranking modal
    const handleCloseCreateModal = () => {
        dispatch({ type: 'CLOSE_CREATE_MODAL' });
    };

    // Handle submission of new ranking data from the modal
    const handleCreateRankingSubmit = async (newRankingData) => {
        try {
            const response = await teamRankingApi.createTeamRanking(newRankingData);
            dispatch({ type: 'ADD_RANKING_SUCCESS', payload: response.data });
            fetchRankingsAndTeams(); // 변경사항 반영을 위해 다시 불러오기
        } catch (err) {
            console.error('팀 순위 등록 실패:', err);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: `팀 순위 등록에 실패했습니다: ${err.response?.data || err.message}` });
        }
    };

    // Handle opening edit ranking modal
    const handleOpenEditModal = (ranking) => {
        dispatch({ type: 'OPEN_EDIT_MODAL', payload: { ...ranking } }); // Pass a copy of ranking data
    };

    // Handle closing edit ranking modal
    const handleCloseEditModal = () => {
        dispatch({ type: 'CLOSE_EDIT_MODAL' });
    };

    // Handle submission of edited ranking data from the modal
    const handleEditRankingSubmit = async (updatedRankingData) => {
        try {
            const response = await teamRankingApi.updateTeamRanking(currentRankingToEdit.id, updatedRankingData);
            dispatch({ type: 'UPDATE_RANKING_SUCCESS', payload: response.data });
            fetchRankingsAndTeams(); // 변경사항 반영을 위해 다시 불러오기
        } catch (err) {
            console.error('팀 순위 정보 수정 실패:', err);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: `팀 순위 정보 수정에 실패했습니다: ${err.response?.data || err.message}` });
        }
    };

    // Handle ranking deletion
    const handleDeleteRanking = async (rankingId, teamName, seasonYear) => {
        // IMPORTANT: Replace window.confirm with a custom modal for better UX and consistency.
        // For now, keeping window.confirm as per original code, but note for future improvement.
        if (window.confirm(`${seasonYear} 시즌 ${teamName} 팀의 순위 정보를 정말로 삭제하시겠습니까?`)) {
            try {
                await teamRankingApi.deleteTeamRanking(rankingId);
                dispatch({ type: 'DELETE_RANKING_SUCCESS', payload: rankingId });
            } catch (err) {
                console.error('팀 순위 삭제 실패:', err);
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: `팀 순위 삭제에 실패했습니다: ${err.response?.data || err.message}` });
            }
        }
    };

    // 순위 계산 및 저장 (이 함수는 더 이상 UI에서 호출되지 않습니다.)
    // const handleCalculateRankings = async () => {
    //     if (window.confirm(`${selectedSeasonYear} 시즌의 팀 순위를 현재 경기 결과에 기반하여 계산하고 저장하시겠습니까? (기존 데이터가 업데이트됩니다.)`)) {
    //         try {
    //             dispatch({ type: 'FETCH_START' }); // 로딩 상태 시작
    //             const response = await teamRankingApi.calculateAndSaveRankings(selectedSeasonYear);
    //             dispatch({ type: 'SET_MODAL_MESSAGE', payload: `${selectedSeasonYear} 시즌 팀 순위가 성공적으로 계산 및 저장되었습니다.` });
    //             dispatch({ type: 'FETCH_SUCCESS', payload: { rankings: response.data, allTeams: allTeams } }); // 새로 계산된 순위로 업데이트
    //         } catch (err) {
    //             console.error('팀 순위 계산 실패:', err);
    //             dispatch({ type: 'SET_MODAL_MESSAGE', payload: `팀 순위 계산에 실패했습니다: ${err.response?.data?.message || err.message}` });
    //         } finally {
    //             dispatch({ type: 'SET_LOADING', payload: false }); // 로딩 상태 종료
    //         }
    //     }
    // };

    // 연도 필터 옵션 생성 (더 이상 UI에서 사용되지 않으므로 제거)
    // const yearOptions = Array.from({ length: 11 }, (_, i) => new Date().getFullYear() - 5 + i);


    if (loading) return <div className="text-center py-4 text-secondary">팀 순위 목록을 불러오는 중...</div>;
    if (error) return <div className="text-center py-4 text-danger fw-bold">오류: {error}</div>;

    return (
        <div className="container my-5 p-4 bg-white rounded-lg shadow-lg">
            <h2 className="text-center text-primary mb-4 pb-2 border-bottom border-primary-subtle">팀 순위 관리</h2>

            <div className="d-flex justify-content-end mb-4"> {/* 연도 필터 div 제거, 새 순위 등록 버튼만 남김 */}
                <button
                    className="btn btn-success rounded-pill px-4 py-2 fw-bold shadow-sm"
                    onClick={handleOpenCreateModal}
                >
                    새 순위 등록
                </button>
            </div>

            {rankings.length === 0 ? (
                <div className="alert alert-info text-center my-4" role="alert">
                    선택된 시즌에 등록된 팀 순위 정보가 없습니다.
                </div>
            ) : (
                <div className="table-responsive">
                    <table className="table table-hover table-striped align-middle">
                        <thead className="table-primary">
                            <tr>
                                <th scope="col" className="text-center">순위</th>
                                <th scope="col" className="text-center">로고</th>
                                <th scope="col">팀 이름</th>
                                <th scope="col" className="text-center">승</th>
                                <th scope="col" className="text-center">패</th>
                                <th scope="col" className="text-center">무</th>
                                <th scope="col" className="text-center">승률</th>
                                <th scope="col" className="text-center">게임차</th>
                                <th scope="col" className="text-center">수정/삭제</th>
                            </tr>
                        </thead>
                        <tbody>
                            {rankings.map((ranking) => (
                                <tr key={ranking.id}>
                                    <td className="text-center fw-bold">{ranking.currentRank}</td>
                                    <td className="text-center">
                                        <img
                                            src={getTeamLogoUrl(ranking.team.name)}
                                            alt={`${ranking.team.name} 로고`}
                                            style={{ width: '40px', height: '40px', objectFit: 'contain' }}
                                            onError={handleImageError}
                                        />
                                    </td>
                                    <td>{ranking.team.name}</td>
                                    <td className="text-center">{ranking.wins}</td>
                                    <td className="text-center">{ranking.losses}</td>
                                    <td className="text-center">{ranking.draws}</td>
                                    <td className="text-center">{ranking.winRate.toFixed(3)}</td>
                                    <td className="text-center">{ranking.gamesBehind.toFixed(1)}</td>
                                    <td className="text-center">
                                        <div className="d-flex gap-2 justify-content-center">
                                            <button
                                                className="btn btn-sm btn-info text-white rounded-pill"
                                                onClick={() => handleOpenEditModal(ranking)}
                                            >
                                                수정
                                            </button>
                                            <button
                                                className="btn btn-sm btn-danger rounded-pill"
                                                onClick={() => handleDeleteRanking(ranking.id, ranking.team.name, ranking.seasonYear)}
                                            >
                                                삭제
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Create Ranking Modal */}
            {isCreateModalOpen && (
                <TeamRankingFormModal
                    title="새 팀 순위 등록"
                    buttonText="등록"
                    onSubmit={handleCreateRankingSubmit}
                    onClose={handleCloseCreateModal}
                    allTeams={allTeams} // 모든 팀 목록 전달
                />
            )}

            {/* Edit Ranking Modal */}
            {isEditModalOpen && currentRankingToEdit && (
                <TeamRankingFormModal
                    title={`팀 순위 수정: ${currentRankingToEdit.team.name}`}
                    buttonText="저장"
                    initialData={currentRankingToEdit}
                    onSubmit={handleEditRankingSubmit}
                    onClose={handleCloseEditModal}
                    allTeams={allTeams} // 모든 팀 목록 전달
                />
            )}

            {/* Generic Message Modal */}
            <Modal message={modalMessage} onClose={handleModalClose} />
        </div>
    );
}

export default AdminTeamRankings;
