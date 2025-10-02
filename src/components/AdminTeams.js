import React, { useEffect, useReducer } from 'react';
import { teamApi } from '../services/apiService'; // Import teamApi
import Modal from './Modal'; // Generic modal for messages
import TeamFormModal from './TeamFormModal'; // New: Import the dedicated team form modal

// 1. Initial State Definition for useReducer
const initialState = {
    teams: [],
    loading: true,
    error: null,
    modalMessage: '',
    isCreateModalOpen: false, // State for create team modal
    isEditModalOpen: false,   // State for edit team modal
    currentTeamToEdit: null,  // Stores the team data being edited
};

// 2. Reducer Function Definition
function adminTeamsReducer(state, action) {
    switch (action.type) {
        case 'FETCH_START':
            return { ...state, loading: true, error: null };
        case 'FETCH_SUCCESS':
            return { ...state, loading: false, teams: action.payload };
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
            return { ...state, isEditModalOpen: true, currentTeamToEdit: action.payload };
        case 'CLOSE_EDIT_MODAL':
            return { ...state, isEditModalOpen: false, currentTeamToEdit: null };
        case 'ADD_TEAM_SUCCESS':
            return {
                ...state,
                teams: [...state.teams, action.payload], // Add new team to the list
                isCreateModalOpen: false,
                modalMessage: '팀이 성공적으로 등록되었습니다.'
            };
        case 'UPDATE_TEAM_SUCCESS':
            // Replace the updated team in the list
            const updatedTeams = state.teams.map(team =>
                team.id === action.payload.id ? action.payload : team
            );
            return {
                ...state,
                teams: updatedTeams,
                isEditModalOpen: false,
                currentTeamToEdit: null,
                modalMessage: '팀 정보가 성공적으로 수정되었습니다.'
            };
        case 'DELETE_TEAM_SUCCESS':
            // Remove the deleted team from the list
            const remainingTeams = state.teams.filter(team => team.id !== action.payload);
            return {
                ...state,
                teams: remainingTeams,
                modalMessage: '팀이 성공적으로 삭제되었습니다.'
            };
        default:
            return state;
    }
}

function AdminTeams() {
    const [state, dispatch] = useReducer(adminTeamsReducer, initialState);
    const { teams, loading, error, modalMessage, isCreateModalOpen, isEditModalOpen, currentTeamToEdit } = state;

    // Function to fetch team list
    const fetchTeams = async () => {
        dispatch({ type: 'FETCH_START' });
        try {
            const response = await teamApi.getAllTeams();
            dispatch({ type: 'FETCH_SUCCESS', payload: response.data });
        } catch (err) {
            console.error('팀 목록 불러오기 실패:', err);
            dispatch({ type: 'FETCH_ERROR', payload: err.message });
        }
    };

    // Fetch teams on component mount
    useEffect(() => {
        fetchTeams();
    }, []); // Empty dependency array means this runs once on mount

    // Handle generic modal close (for messages)
    const handleModalClose = () => {
        dispatch({ type: 'CLEAR_MODAL_MESSAGE' });
    };

    // Handle opening create team modal
    const handleOpenCreateModal = () => {
        dispatch({ type: 'OPEN_CREATE_MODAL' });
    };

    // Handle closing create team modal
    const handleCloseCreateModal = () => {
        dispatch({ type: 'CLOSE_CREATE_MODAL' });
    };

    // Handle submission of new team data from the modal
    const handleCreateTeamSubmit = async (newTeamData) => {
        try {
            const response = await teamApi.createTeam(newTeamData);
            dispatch({ type: 'ADD_TEAM_SUCCESS', payload: response.data });
            // FIX: No need to call fetchTeams() here as the reducer already added the new team
            // fetchTeams(); 
        } catch (err) {
            console.error('팀 등록 실패:', err);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: `팀 등록에 실패했습니다: ${err.response?.data || err.message}` });
        }
    };

    // Handle opening edit team modal
    const handleOpenEditModal = (team) => {
        dispatch({ type: 'OPEN_EDIT_MODAL', payload: { ...team } }); // Pass a copy of team data
    };

    // Handle closing edit team modal
    const handleCloseEditModal = () => {
        dispatch({ type: 'CLOSE_EDIT_MODAL' });
    };

    // Handle submission of edited team data from the modal
    const handleEditTeamSubmit = async (updatedTeamData) => {
        try {
            const response = await teamApi.updateTeam(currentTeamToEdit.id, updatedTeamData);
            dispatch({ type: 'UPDATE_TEAM_SUCCESS', payload: response.data });
            // FIX: No need to call fetchTeams() here as the reducer already updated the team
            // fetchTeams(); 
        } catch (err) {
            console.error('팀 정보 수정 실패:', err);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: `팀 정보 수정에 실패했습니다: ${err.response?.data || err.message}` });
        }
    };

    // Handle team deletion
    const handleDeleteTeam = async (teamId, teamName) => {
        // IMPORTANT: Replace window.confirm with a custom modal for better UX and consistency.
        // For now, keeping window.confirm as per original code, but note for future improvement.
        if (window.confirm(`${teamName} 팀을 정말로 삭제하시겠습니까?`)) {
            try {
                await teamApi.deleteTeam(teamId);
                dispatch({ type: 'DELETE_TEAM_SUCCESS', payload: teamId });
                // No need to fetchMembers() here as the reducer already removed it
            } catch (err) {
                console.error('팀 삭제 실패:', err);
                // Handle specific error for associated entities if needed
                if (err.response && err.response.status === 409) { // Example: Conflict if team has associated games
                    dispatch({ type: 'SET_MODAL_MESSAGE', payload: `팀 삭제 실패: ${teamName} 팀에 연결된 경기 일정이 있습니다. 관련 일정을 먼저 삭제해주세요.` });
                } else {
                    dispatch({ type: 'SET_MODAL_MESSAGE', payload: `팀 삭제에 실패했습니다: ${err.response?.data || err.message}` });
                }
            }
        }
    };

    if (loading) return <div className="text-center py-4 text-secondary">팀 목록을 불러오는 중...</div>;
    if (error) return <div className="text-center py-4 text-danger fw-bold">오류: {error}</div>;

    return (
        <div className="container my-5 p-4 bg-white rounded-lg shadow-lg">
            <h2 className="text-center text-primary mb-4 pb-2 border-bottom border-primary-subtle">팀 관리</h2>

            <div className="d-flex justify-content-end mb-4">
                <button
                    className="btn btn-success rounded-pill px-4 py-2 fw-bold shadow-sm"
                    onClick={handleOpenCreateModal}
                >
                    새 팀 등록
                </button>
            </div>

            {teams.length === 0 ? (
                <div className="alert alert-info text-center my-4" role="alert">등록된 팀이 없습니다.</div>
            ) : (
                <div className="table-responsive">
                    <table className="table table-hover table-striped align-middle">
                        <thead className="table-primary">
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">로고</th>
                                <th scope="col">팀 이름</th>
                                <th scope="col">로고 URL</th>
                                <th scope="col">수정/삭제</th>
                            </tr>
                        </thead>
                        <tbody>
                            {teams.map((team) => (
                                <tr key={team.id}>
                                    <td>{team.id}</td>
                                    <td>
                                        <img
                                            src={team.logoUrl}
                                            alt={`${team.name} 로고`}
                                            style={{ width: '50px', height: '50px', objectFit: 'contain' }}
                                            onError={(e) => { e.target.onerror = null; e.target.src = 'https://placehold.co/50x50/CCCCCC/000000?text=Logo'; }}
                                        />
                                    </td>
                                    <td>{team.name}</td>
                                    <td>{team.logoUrl}</td>
                                    <td>
                                        <div className="d-flex gap-2">
                                            <button
                                                className="btn btn-sm btn-info text-white rounded-pill"
                                                onClick={() => handleOpenEditModal(team)}
                                            >
                                                수정
                                            </button>
                                            <button
                                                className="btn btn-sm btn-danger rounded-pill"
                                                onClick={() => handleDeleteTeam(team.id, team.name)}
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

            {/* Create Team Modal */}
            {isCreateModalOpen && (
                <TeamFormModal
                    title="새 팀 등록"
                    buttonText="등록"
                    onSubmit={handleCreateTeamSubmit}
                    onClose={handleCloseCreateModal}
                />
            )}

            {/* Edit Team Modal */}
            {isEditModalOpen && currentTeamToEdit && (
                <TeamFormModal
                    title={`팀 수정: ${currentTeamToEdit.name}`}
                    buttonText="저장"
                    initialData={currentTeamToEdit}
                    onSubmit={handleEditTeamSubmit}
                    onClose={handleCloseEditModal}
                />
            )}

            {/* Generic Message Modal */}
            <Modal message={modalMessage} onClose={handleModalClose} />
        </div>
    );
}

export default AdminTeams;
