import React, { useReducer, useEffect, useCallback, useContext } from 'react';
import { commentApi } from '../services/apiService';
import { AuthContext } from '../context/AuthContext';
import Modal from './Modal';

// --- Reducer setup ---
const initialState = {
    comments: [],
    newCommentText: '',
    newCommentType: 'TEXT',
    predictedTeamName: '',
    predictionCounts: {},
    editingCommentId: null,
    editingCommentText: '',
    editingCommentType: '',
    editingPredictedTeamName: '',
    modalMessage: '',
    showConfirmDeleteModal: false,
    commentToDelete: null,
    commentFilter: 'ALL',
    currentPage: 0, 
    commentsPerPage: 10, 
    totalComments: 0,
};

const reducer = (state, action) => {
    switch (action.type) {
        case 'SET_PAGE_DATA':
            return { 
                ...state, 
                comments: action.payload.content, 
                totalComments: action.payload.totalElements,
                currentPage: action.payload.number, 
            };
        case 'SET_NEW_COMMENT_TEXT':
            return { ...state, newCommentText: action.payload };
        case 'SET_NEW_COMMENT_TYPE':
            return { ...state, newCommentType: action.payload, predictedTeamName: action.payload === 'TEXT' ? '' : state.predictedTeamName };
        case 'SET_PREDICTED_TEAM_NAME':
            return { ...state, predictedTeamName: action.payload };
        case 'SET_PREDICTION_COUNTS':
            return { ...state, predictionCounts: action.payload };
        case 'START_EDITING':
            return {
                ...state,
                editingCommentId: action.payload.id,
                editingCommentText: action.payload.commentText,
                editingCommentType: action.payload.type,
                editingPredictedTeamName: action.payload.predictedTeamName || '',
            };
        case 'CANCEL_EDITING':
            return {
                ...state,
                editingCommentId: null,
                editingCommentText: '',
                editingCommentType: '',
                editingPredictedTeamName: '',
            };
        case 'SET_EDITING_COMMENT_TEXT':
            return { ...state, editingCommentText: action.payload };
        case 'SET_EDITING_COMMENT_TYPE':
            return { ...state, editingCommentType: action.payload, editingPredictedTeamName: action.payload === 'TEXT' ? '' : state.editingPredictedTeamName };
        case 'SET_EDITING_PREDICTED_TEAM_NAME':
            return { ...state, editingPredictedTeamName: action.payload };
        case 'RESET_NEW_COMMENT_FORM':
            return { ...state, newCommentText: '', newCommentType: 'TEXT', predictedTeamName: '' };
        case 'SET_MODAL_MESSAGE':
            return { ...state, modalMessage: action.payload };
        case 'SET_SHOW_CONFIRM_DELETE_MODAL':
            return { ...state, showConfirmDeleteModal: action.payload };
        case 'SET_COMMENT_TO_DELETE':
            return { ...state, commentToDelete: action.payload };
        case 'CLOSE_MODAL':
            return { ...state, modalMessage: '', showConfirmDeleteModal: false, commentToDelete: null };
        case 'SET_COMMENT_FILTER':
            return { ...state, commentFilter: action.payload, currentPage: 0 };
        default:
            throw new Error(`알 수 없는 액션 타입: ${action.type}`);
    }
};

// --- End of Reducer setup ---

const CommentSection = ({ gameId, currentUser, isAdmin, homeTeamName, opponentTeamName }) => {
    const [state, dispatch] = useReducer(reducer, initialState);
    // eslint-disable-next-line no-unused-vars
    const { isAuthenticated, user } = useContext(AuthContext);
    const {
        comments,
        newCommentText,
        newCommentType,
        predictedTeamName,
        predictionCounts,
        editingCommentId,
        editingCommentText,
        editingCommentType,
        editingPredictedTeamName,
        modalMessage,
        showConfirmDeleteModal,
        commentToDelete,
        commentFilter,
        currentPage,
        commentsPerPage,
        totalComments,
    } = state;

    // ⭐️ 댓글 타입 텍스트를 생성하는 함수
    const getPredictionText = (comment) => {
        if (comment.type === 'PREDICTION' && comment.predictedTeamName) {
            return `[예측: ${comment.predictedTeamName}]`;
        }
        return '[일반]';
    };

   
    const getBadgeClass = (comment) => {
        if (comment.type === 'PREDICTION') {
            if (comment.predictedTeamName === homeTeamName) {
               
                return 'bg-success-subtle text-dark'; 
            } else if (comment.predictedTeamName === opponentTeamName) {
                
                return 'bg-danger-subtle text-dark'; 
            }
        }
        
        return 'bg-secondary text-white';
    };

    const fetchComments = useCallback(async (page = currentPage, size = commentsPerPage) => {
        try {
            const response = await commentApi.getCommentsByGameId(gameId, page, size);
            const pageData = response.data; 
            const commentsList = Array.isArray(pageData.content) ? pageData.content : [];

            dispatch({ 
                type: 'SET_PAGE_DATA', 
                payload: {
                    content: commentsList,
                    number: pageData.number, 
                    totalElements: pageData.totalElements,
                }
            });

        } catch (error) {
            console.error('댓글을 불러오는 데 실패했습니다:', error);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '댓글을 불러오는 데 실패했습니다.' });
            dispatch({ 
                type: 'SET_PAGE_DATA', 
                payload: { content: [], number: 0, totalElements: 0 } 
            });
        }
    }, [gameId, currentPage, commentsPerPage]); 

    const fetchPredictionCounts = useCallback(async () => {
        try {
            const response = await commentApi.getPredictionCommentCounts(gameId);
            dispatch({ type: 'SET_PREDICTION_COUNTS', payload: response.data });
        } catch (error) {
            console.error('예측 댓글 수를 불러오는 데 실패했습니다:', error);
        }
    }, [gameId]);

    useEffect(() => {
        if (gameId) {
            fetchComments(currentPage, commentsPerPage); 
            fetchPredictionCounts();
        }
    }, [gameId, fetchComments, fetchPredictionCounts, currentPage, commentsPerPage, commentFilter]); 

    const handleAddComment = async (e) => {
        e.preventDefault();

        if (!isAuthenticated) {
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '로그인 후 댓글을 작성할 수 있습니다.' });
            return;
        }
        if (!newCommentText.trim()) {
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '댓글 내용을 입력해주세요.' });
            return;
        }
        if (newCommentType === 'PREDICTION' && !predictedTeamName.trim()) {
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '예측 댓글은 예측 팀을 선택해야 합니다.' });
            return;
        }

        const commentData = {
            commentText: newCommentText.trim(),
            type: newCommentType,
            predictedTeamName: newCommentType === 'PREDICTION' ? predictedTeamName.trim() : null,
            username: user?.username,
            nickname: user?.nickname
        };

        try {
            await commentApi.addComment(gameId, commentData);
            dispatch({ type: 'RESET_NEW_COMMENT_FORM' });
            fetchComments(0, commentsPerPage); 
            fetchPredictionCounts();
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '댓글이 성공적으로 추가되었습니다.' });
        } catch (error) {
            console.error('댓글 추가 실패:', error);
            const errorMessage = error.response?.data?.message || '댓글 추가에 실패했습니다.';
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: errorMessage });
        }
    };

    const handleUpdateComment = async (commentId) => {
        if (!editingCommentText.trim()) {
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '댓글 내용을 입력해주세요.' });
            return;
        }
        if (editingCommentType === 'PREDICTION' && !editingPredictedTeamName.trim()) {
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '예측 댓글은 예측 팀을 선택해야 합니다.' });
            return;
        }

        const updatedData = {
            commentText: editingCommentText.trim(),
            type: editingCommentType,
            predictedTeamName: editingCommentType === 'PREDICTION' ? editingPredictedTeamName.trim() : null,
            username: user?.username,
            nickname: user?.nickname
        };

        try {
            await commentApi.updateComment(gameId, commentId, updatedData);
            dispatch({ type: 'CANCEL_EDITING' });
            fetchComments(currentPage, commentsPerPage);
            fetchPredictionCounts();
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '댓글이 성공적으로 수정되었습니다.' });
        } catch (error) {
            console.error('댓글 수정 실패:', error);
            const errorMessage = error.response?.data?.message || '댓글 수정에 실패했습니다.';
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: errorMessage });
        }
    };

    const handleDeleteClick = (comment) => {
        dispatch({ type: 'SET_COMMENT_TO_DELETE', payload: comment });
        dispatch({ type: 'SET_SHOW_CONFIRM_DELETE_MODAL', payload: true });
    };

    const confirmDeleteComment = async () => {
        if (!commentToDelete) return;
        try {
            await commentApi.deleteComment(gameId, commentToDelete.id);
            const newCurrentPage = (filteredComments.length === 1 && currentPage > 0) ? currentPage - 1 : currentPage;
            fetchComments(newCurrentPage, commentsPerPage);
            fetchPredictionCounts();
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '댓글이 성공적으로 삭제되었습니다.' });
        } catch (error) {
            console.error('댓글 삭제 실패:', error);
            const errorMessage = error.response?.data?.message || '댓글 삭제에 실패했습니다.';
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: errorMessage });
        } finally {
            dispatch({ type: 'CLOSE_MODAL' });
        }
    };

    const formatDate = (isoString) => {
        if (!isoString) return '';
        const date = new Date(isoString);
        return date.toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    const totalPredictions = Object.values(predictionCounts).reduce((sum, count) => sum + count, 0);
    const homeTeamPredictionCount = predictionCounts[homeTeamName] || 0;
    const opponentTeamPredictionCount = predictionCounts[opponentTeamName] || 0;

    const homeTeamPredictionPercentage = totalPredictions > 0 ? (homeTeamPredictionCount / totalPredictions) * 100 : 50;
    const opponentTeamPredictionPercentage = totalPredictions > 0 ? (opponentTeamPredictionCount / totalPredictions) * 100 : 50;

    const filteredComments = (Array.isArray(comments) ? comments : []).filter(comment => {
        if (commentFilter === 'ALL') {
            return true;
        } else if (commentFilter === homeTeamName) {
            return comment.type === 'PREDICTION' && comment.predictedTeamName === homeTeamName;
        } else if (commentFilter === opponentTeamName) {
            return comment.type === 'PREDICTION' && comment.predictedTeamName === opponentTeamName;
        } else if (commentFilter === 'TEXT') {
            return comment.type === 'TEXT';
        }
        return false;
    });

    const totalPages = Math.ceil(totalComments / commentsPerPage);
    const pageNumbers = [];
    for (let i = 1; i <= totalPages; i++) {
        pageNumbers.push(i);
    }

    const handlePageChange = (pageNumber) => {
        const backendPageNumber = pageNumber - 1;
        fetchComments(backendPageNumber, commentsPerPage);
    };

    return (
        <div className="container my-4">
              <h3 className="text-center mb-4 visually-hidden">댓글</h3>
            <hr className="my-4 border-0" />

            {/* Prediction Status Section */}
            <div className="card mb-4 shadow-sm">
                <div className="card-body bg-info-subtle rounded">
                    <h4 className="card-title text-center mb-3">승부 예측 현황:</h4>
                    <div className="d-flex justify-content-between mb-2 px-2">
                        <span className="text-primary fw-bold">{homeTeamName}</span>
                        <span className="text-danger fw-bold">{opponentTeamName}</span>
                    </div>

                    <div className="progress position-relative" style={{ height: '30px' }}>
                        <div
                            className="progress-bar bg-success"
                            role="progressbar"
                            style={{ width: `${homeTeamPredictionPercentage}%` }}
                            aria-valuenow={homeTeamPredictionPercentage}
                            aria-valuemin="0"
                            aria-valuemax="100"
                        >
                            {homeTeamPredictionCount > 0 && `${homeTeamPredictionCount}개`}
                        </div>
                        <div
                            className="progress-bar bg-danger"
                            role="progressbar"
                            style={{ width: `${opponentTeamPredictionPercentage}%` }}
                            aria-valuenow={opponentTeamPredictionPercentage}
                            aria-valuemin="0"
                            aria-valuemax="100"
                        >
                            {opponentTeamPredictionCount > 0 && `${opponentTeamPredictionCount}개`}
                        </div>
                    </div>
                    <div className="text-start mt-2 ms-2">
                        총 {totalPredictions}건
                    </div>
                </div>
            </div>

            {/* Add New Comment Form */}
            <div className="card mb-4 shadow-sm">
                <div className="card-body">
                    <form onSubmit={handleAddComment}>
                        <div className="mb-3">
                            <textarea
                                className="form-control"
                                placeholder="새 댓글을 입력하세요..."
                                value={newCommentText}
                                onChange={(e) => dispatch({ type: 'SET_NEW_COMMENT_TEXT', payload: e.target.value })}
                                rows="3"
                            ></textarea>
                        </div>
                        <div className="d-flex justify-content-between align-items-center">
                            <div className="col-auto" style={{ width: '120px' }}>
                                <select
                                    className="form-select form-select-sm"
                                    value={newCommentType}
                                    onChange={(e) => dispatch({ type: 'SET_NEW_COMMENT_TYPE', payload: e.target.value })}
                                >
                                    <option value="TEXT">일반 댓글</option>
                                    <option value="PREDICTION">예측 댓글</option>
                                </select>
                            </div>

                            {newCommentType === 'PREDICTION' && (
                                <div className="col-auto ms-2" style={{ width: '120px' }}>
                                    <select
                                        className="form-select form-select-sm"
                                        value={predictedTeamName}
                                        onChange={(e) => dispatch({ type: 'SET_PREDICTED_TEAM_NAME', payload: e.target.value })}
                                    >
                                        <option value="">예측할 팀 선택</option>
                                        {homeTeamName && <option value={homeTeamName}>{homeTeamName}</option>}
                                        {opponentTeamName && <option value={opponentTeamName}>{opponentTeamName}</option>}
                                    </select>
                                </div>
                            )}
                            <button type="submit" className="btn btn-primary px-4 fw-bold ms-auto">댓글 추가</button>
                        </div>
                    </form>
                </div>
            </div>

            {/* Comments List */}
            <div className="card shadow-sm">
                <div className="card-body">
                    <div className="d-flex justify-content-end align-items-center mb-2">
                        <div className="col-auto">
                            <select
                                className="form-select form-select-sm"
                                value={commentFilter}
                                onChange={(e) => dispatch({ type: 'SET_COMMENT_FILTER', payload: e.target.value })}
                            >
                                <option value="ALL">모든 댓글</option>
                                <option value={homeTeamName}>{homeTeamName} 예측 댓글</option>
                                <option value={opponentTeamName}>{opponentTeamName} 예측 댓글</option>
                                <option value="TEXT">일반 댓글</option>
                            </select>
                        </div>
                    </div>
                    <hr className="my-2 border-0" />

                    {filteredComments.length === 0 && totalComments > 0 ? (
                        <p className="text-center text-muted">해당 필터에 맞는 댓글이 없습니다.</p>
                    ) : filteredComments.length === 0 && totalComments === 0 ? (
                        <p className="text-center text-muted">아직 댓글이 없습니다.</p>
                    ) : (
                        <ul className="list-unstyled">
                            {filteredComments.map((comment) => (
                                <li key={comment.id} className="card mb-3 shadow-sm border-0">
                                    <div className="card-body">
                                        {editingCommentId === comment.id ? (
                                            // Editing mode (Same as before)
                                            <div>
                                                <div className="mb-3">
                                                    <textarea
                                                        className="form-control"
                                                        value={editingCommentText}
                                                        onChange={(e) => dispatch({ type: 'SET_EDITING_COMMENT_TEXT', payload: e.target.value })}
                                                        rows="3"
                                                    ></textarea>
                                                </div>
                                                <div className="mb-3 d-flex gap-2">
                                                    <select
                                                        className="form-select flex-grow-1"
                                                        value={editingCommentType}
                                                        onChange={(e) => dispatch({ type: 'SET_EDITING_COMMENT_TYPE', payload: e.target.value })}
                                                    >
                                                        <option value="TEXT">일반 댓글</option>
                                                        <option value="PREDICTION">예측 댓글</option>
                                                    </select>
                                                    {editingCommentType === 'PREDICTION' && (
                                                        <select
                                                            className="form-select flex-grow-1"
                                                            value={editingPredictedTeamName}
                                                            onChange={(e) => dispatch({ type: 'SET_EDITING_PREDICTED_TEAM_NAME', payload: e.target.value })}
                                                        >
                                                            <option value="">예측할 팀 선택</option>
                                                            {homeTeamName && <option value={homeTeamName}>{homeTeamName}</option>}
                                                            {opponentTeamName && <option value={opponentTeamName}>{opponentTeamName}</option>}
                                                        </select>
                                                    )}
                                                </div>
                                                <div className="d-flex justify-content-end gap-2">
                                                    <button onClick={() => handleUpdateComment(comment.id)} className="btn btn-success btn-sm">저장</button>
                                                    <button onClick={() => dispatch({ type: 'CANCEL_EDITING' })} className="btn btn-secondary btn-sm">취소</button>
                                                </div>
                                            </div>
                                        ) : (
                                            // Display mode
                                            <>
                                                <div className="d-flex justify-content-between align-items-start mb-2">
                                                    {/* Left side: Nickname, Badge, and Comment Text */}
                                                    <div className="d-flex flex-column align-items-start flex-grow-1">
                                                        
                                                        {/* Combined Nickname and Badge (Horizontal placement) */}
                                                        <div className="d-flex align-items-center mb-2">
                                                            {/* Nickname */}
                                                            <strong className="me-2">{comment.nickname || comment.username}</strong>
                                                            
                                                            {/* Badge (now placed next to the nickname) */}
                                                            <span 
                                                                className={`badge ${getBadgeClass(comment)}`}
                                                                style={{ fontSize: '0.8em', fontWeight: 'normal' }}
                                                            >
                                                                {getPredictionText(comment)}
                                                            </span>
                                                        </div>

                                                        {/* Comment Text */}
                                                        <div className="text-start">
                                                            {comment.commentText}
                                                        </div>
                                                    </div>

                                                    {/* Right side: Date and controls */}
                                                    <div className="d-flex flex-column align-items-end">
                                                        <small className="text-muted mb-1">{formatDate(comment.createdAt)}</small>
                                                        <div className="d-flex gap-2">
                                                            {(currentUser === comment.username || isAdmin) && (
                                                                <>
                                                                    <button onClick={() => dispatch({ type: 'START_EDITING', payload: comment })} className="btn btn-primary btn-sm">수정</button>
                                                                    <button onClick={() => handleDeleteClick(comment)} className="btn btn-danger btn-sm">삭제</button>
                                                                </>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            </>
                                        )}
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
                <nav aria-label="Comment pagination" className="mt-4">
                    <ul className="pagination justify-content-center">
                        <li className={`page-item ${currentPage === 0 ? 'disabled' : ''}`}>
                            <button 
                                className="page-link" 
                                onClick={() => handlePageChange(currentPage)} 
                                disabled={currentPage === 0}
                            >
                                이전
                            </button>
                        </li>
                        {pageNumbers.map(number => (
                            <li key={number} className={`page-item ${currentPage + 1 === number ? 'active' : ''}`}>
                                <button 
                                    className="page-link" 
                                    onClick={() => handlePageChange(number)} 
                                >
                                    {number}
                                </button>
                            </li>
                        ))}
                        <li className={`page-item ${currentPage + 1 === totalPages ? 'disabled' : ''}`}>
                            <button 
                                className="page-link" 
                                onClick={() => handlePageChange(currentPage + 2)} 
                                disabled={currentPage + 1 === totalPages}
                            >
                                다음
                            </button>
                        </li>
                    </ul>
                </nav>
            )}

            {/* Modals */}
            {modalMessage && (
                <Modal
                    message={modalMessage}
                    onClose={() => dispatch({ type: 'CLOSE_MODAL' })}
                    title="알림"
                />
            )}
            {showConfirmDeleteModal && (
                <Modal
                    title="댓글 삭제 확인"
                    message="정말로 이 댓글을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다."
                    onClose={() => dispatch({ type: 'CLOSE_MODAL' })}
                    onConfirm={confirmDeleteComment}
                    showConfirmButton={true}
                />
            )}
        </div>
    );
};

export default CommentSection;  