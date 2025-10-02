import React, { useEffect,useReducer } from 'react';
import { adminMemberApi } from '../services/apiService';
import Modal from './Modal'; // Generic modal component
import MemberEditModal from './MemberEditModal'; // New: Import the dedicated edit modal

// 1. Initial State Definition for useReducer
const initialState = {
    members: [],
    loading: true,
    error: null,
    modalMessage: '',
    isEditModalOpen: false,
    currentMemberToEdit: null, // Stores the member data being edited
};

// 2. Reducer Function Definition
function adminMembersReducer(state, action) {
    switch (action.type) {
        case 'FETCH_START':
            return { ...state, loading: true, error: null };
        case 'FETCH_SUCCESS':
            return { ...state, loading: false, members: action.payload };
        case 'FETCH_ERROR':
            return { ...state, loading: false, error: action.payload, modalMessage: '회원 목록을 불러오는 데 실패했습니다.' };
        case 'SET_MODAL_MESSAGE':
            return { ...state, modalMessage: action.payload };
        case 'CLEAR_MODAL_MESSAGE':
            return { ...state, modalMessage: '' };
        case 'OPEN_EDIT_MODAL':
            return { ...state, isEditModalOpen: true, currentMemberToEdit: action.payload };
        case 'CLOSE_EDIT_MODAL':
            return { ...state, isEditModalOpen: false, currentMemberToEdit: null };
        case 'UPDATE_MEMBER_SUCCESS':
            // Replace the updated member in the list
            const updatedMembers = state.members.map(member =>
                member.id === action.payload.id ? action.payload : member
            );
            return {
                ...state,
                members: updatedMembers,
                isEditModalOpen: false,
                currentMemberToEdit: null,
                modalMessage: '회원 정보가 성공적으로 수정되었습니다.'
            };
        default:
            return state;
    }
}

function AdminMembers() {
    const [state, dispatch] = useReducer(adminMembersReducer, initialState);
    const { members, loading, error, modalMessage, isEditModalOpen, currentMemberToEdit } = state;

    // Function to fetch member list
    const fetchMembers = async () => {
        dispatch({ type: 'FETCH_START' });
        try {
            const response = await adminMemberApi.getAllMembers();
            dispatch({ type: 'FETCH_SUCCESS', payload: response.data });
        } catch (err) {
            console.error('회원 목록 불러오기 실패:', err);
            dispatch({ type: 'FETCH_ERROR', payload: err.message });
        }
    };

    // Fetch members on component mount
    useEffect(() => {
        fetchMembers();
    }, []);

    // Handle generic modal close (for messages)
    const handleModalClose = () => {
        dispatch({ type: 'CLEAR_MODAL_MESSAGE' });
    };

    // Handle member deletion
    const handleDeleteMember = async (memberId, username) => {
        // IMPORTANT: Do NOT use window.confirm or window.alert. Use a custom Modal component for user confirmation.
        // For now, I'm keeping it as is based on your previous code, but it should be replaced.
        if (window.confirm(`${username} 회원을 정말로 삭제하시겠습니까?`)) {
            try {
                await adminMemberApi.deleteMember(memberId);
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: `${username} 회원이 성공적으로 삭제되었습니다.` });
                fetchMembers(); // Refresh the list
            } catch (err) {
                console.error('회원 삭제 실패:', err);
                if (err.response && err.response.status === 403) {
                    dispatch({ type: 'SET_MODAL_MESSAGE', payload: '관리자 계정은 삭제할 수 없습니다.' });
                } else {
                    dispatch({ type: 'SET_MODAL_MESSAGE', payload: '회원 삭제에 실패했습니다.' });
                }
            }
        }
    };

    // Handle member ban/unban
    const handleBanUnban = async (username, isBanned) => {
        // IMPORTANT: Do NOT use prompt. Use a custom Modal component for user input.
        // For now, I'm keeping it as is based on your previous code, but it should be replaced.
        try {
            if (isBanned) { // If currently banned, unban
                await adminMemberApi.unbanMember(username);
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: `${username} 회원이 정지 해제되었습니다.` });
            } else { // If not banned, ban
                const banDuration = prompt("몇 일 동안 정지하시겠습니까? (영구 정지는 'permanent' 입력)");
                if (banDuration === null) return; // Cancelled
                if (banDuration.toLowerCase() === 'permanent') {
                    await adminMemberApi.banMemberPermanently(username);
                    dispatch({ type: 'SET_MODAL_MESSAGE', payload: `${username} 회원이 영구 정지되었습니다.` });
                } else {
                    const days = parseInt(banDuration, 10);
                    if (isNaN(days) || days <= 0) {
                        dispatch({ type: 'SET_MODAL_MESSAGE', payload: '유효한 정지 기간을 입력해주세요.' });
                        return;
                    }
                    // apiService에서 untilDate를 계산하도록 days만 전달
                    await adminMemberApi.banMemberTemporarily(username, days);
                    dispatch({ type: 'SET_MODAL_MESSAGE', payload: `${username} 회원이 ${days}일 동안 정지되었습니다.` });
                }
            }
            fetchMembers(); // Refresh the list
        } catch (err) {
            console.error('회원 정지/해제 실패:', err);
            if (err.response && err.response.status === 403) {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: '관리자 계정은 정지/해제할 수 없습니다.' });
            } else {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: `회원 정지/해제에 실패했습니다: ${err.response?.data || err.message}` });
            }
        }
    };

    // Open edit modal handler
    const handleOpenEditModal = (member) => {
        dispatch({ type: 'OPEN_EDIT_MODAL', payload: { ...member } }); // Pass a copy of member data
    };

    // Close edit modal handler
    const handleCloseEditModal = () => {
        dispatch({ type: 'CLOSE_EDIT_MODAL' });
    };

    // Handle submission of edited member data from the modal
    const handleEditSubmit = async (updatedMemberData) => {
        try {
            const response = await adminMemberApi.updateMember(currentMemberToEdit.id, updatedMemberData);
            dispatch({ type: 'UPDATE_MEMBER_SUCCESS', payload: response.data });
            fetchMembers(); // Refresh the list to reflect changes
        } catch (err) {
            console.error('회원 정보 수정 실패:', err);
            if (err.response && err.response.status === 409) {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: err.response.data || '중복된 닉네임, 이메일 또는 휴대전화번호입니다.' });
            } else {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: `회원 정보 수정에 실패했습니다: ${err.response?.data || err.message}` });
            }
        }
    };

    if (loading) return <div className="text-center py-4 text-secondary">회원 목록을 불러오는 중...</div>;
    if (error) return <div className="text-center py-4 text-danger fw-bold">오류: {error}</div>;

    return (
        <div className="container my-5 p-4 bg-white rounded-lg shadow-lg">
            <h2 className="text-center text-primary mb-4 pb-2 border-bottom border-primary-subtle">회원 관리</h2>

            {members.length === 0 ? (
                <div className="alert alert-info text-center my-4" role="alert">등록된 회원이 없습니다.</div>
            ) : (
                <div className="table-responsive">
                    <table className="table table-hover table-striped align-middle">
                        <thead className="table-primary">
                            <tr>
                                <th scope="col">ID</th>
                                <th scope="col">아이디</th>
                                <th scope="col">이름</th>
                                <th scope="col">닉네임</th>
                                <th scope="col">이메일</th>
                                <th scope="col">전화번호</th>
                                <th scope="col">역할</th>
                                <th scope="col">상태</th>
                                <th scope="col">정지 기한</th>
                                <th scope="col">수정/정지/삭제</th>
                            </tr>
                        </thead>
                        <tbody>
                            {members.map((member) => (
                                <tr key={member.id}>
                                    <td>{member.id}</td>
                                    <td>{member.username}</td>
                                    <td>{member.name}</td>
                                    <td>{member.nickname}</td>
                                    <td>{member.email}</td>
                                    <td>{member.phoneNumber}</td>
                                    <td>{member.role}</td>
                                    <td>
                                        {member.banned ? (
                                            <span className="badge bg-danger">정지</span>
                                        ) : (
                                            <span className="badge bg-success">활성</span>
                                        )}
                                    </td>
                                    <td>
                                        {member.banned && member.bannedUntil ?
                                            new Date(member.bannedUntil).toLocaleString() :
                                            (member.banned ? '영구 정지' : '-')
                                        }
                                    </td>
                                    <td>
                                        <div className="d-flex gap-2">
                                            <button
                                                className="btn btn-sm btn-info text-white rounded-pill"
                                                onClick={() => handleOpenEditModal(member)}
                                            >
                                                수정
                                            </button>
                                            <button
                                                className={`btn btn-sm ${member.banned ? 'btn-success' : 'btn-warning'} rounded-pill`}
                                                onClick={() => handleBanUnban(member.username, member.banned)}
                                            >
                                                {member.banned ? '정지 해제' : '정지'}
                                            </button>
                                            <button
                                                className="btn btn-sm btn-danger rounded-pill"
                                                onClick={() => handleDeleteMember(member.id, member.username)}
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

            {/* Member Edit Modal */}
            {isEditModalOpen && currentMemberToEdit && (
                <MemberEditModal
                    member={currentMemberToEdit}
                    onClose={handleCloseEditModal}
                    onSubmit={handleEditSubmit}
                />
            )}

            <Modal message={modalMessage} onClose={handleModalClose} />
        </div>
    );
}

export default AdminMembers;
