import React, { useEffect, useCallback, useContext, useReducer } from 'react';
import { teamRankingApi, teamApi } from '../services/apiService'; // apiService 경로 확인
import Modal from './Modal'; // Modal 컴포넌트 경로 확인
import { Link } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext'; // AuthContext 경로 확인

// --- Reducer 관련 정의 시작 ---

// 1. 상태를 정의합니다.
const initialState = {
    rankings: [],
    allTeams: [], // 팀 로고 매핑을 위해 모든 팀 데이터 유지
    loading: true, // 초기 데이터 로딩 상태 (컴포넌트 마운트 시 데이터 로딩)
    actionLoading: false, // 특정 관리자 액션 (크롤링) 로딩 상태
    error: null, // 오류 객체 또는 메시지를 저장
    modalMessage: '',
};

// 2. 액션 타입을 정의합니다.
const ActionTypes = {
    FETCH_START: 'FETCH_START',           // 데이터 불러오기 시작
    FETCH_SUCCESS: 'FETCH_SUCCESS',       // 데이터 불러오기 성공
    FETCH_ERROR: 'FETCH_ERROR',           // 데이터 불러오기 실패
    ACTION_START: 'ACTION_START',         // 관리자 액션 (크롤링) 시작
    ACTION_SUCCESS: 'ACTION_SUCCESS',     // 관리자 액션 성공
    ACTION_ERROR: 'ACTION_ERROR',         // 관리자 액션 실패
    SET_MODAL_MESSAGE: 'SET_MODAL_MESSAGE', // 모달 메시지 설정
    CLEAR_MODAL_MESSAGE: 'CLEAR_MODAL_MESSAGE', // 모달 메시지 초기화
};

// 3. Reducer 함수를 정의합니다.
function rankingReducer(state, action) {
    switch (action.type) {
        case ActionTypes.FETCH_START:
            return {
                ...state,
                loading: true,
                error: null, // 새로운 fetch 시작 시 기존 에러 초기화
                modalMessage: '', // 새로운 fetch 시작 시 기존 모달 메시지 클리어
            };
        case ActionTypes.FETCH_SUCCESS:
            return {
                ...state,
                loading: false,
                rankings: action.payload.rankings,
                allTeams: action.payload.allTeams,
                error: null,
            };
        case ActionTypes.FETCH_ERROR:
            return {
                ...state,
                loading: false,
                error: action.payload.error, // 에러 객체 또는 메시지 저장
                modalMessage: action.payload.modalMessage, // 에러 메시지를 모달에 표시
                rankings: [], // 에러 발생 시 순위 데이터 초기화
            };
        case ActionTypes.ACTION_START:
            return {
                ...state,
                actionLoading: true,
                error: null, // 액션 시작 시 기존 에러 초기화
                modalMessage: '작업을 처리 중입니다...', // 작업 시작 시 메시지
            };
        case ActionTypes.ACTION_SUCCESS:
            return {
                ...state,
                actionLoading: false,
                rankings: action.payload.rankings, // 업데이트된 순위 데이터로 교체
                modalMessage: action.payload.modalMessage, // 성공 메시지를 모달에 표시
                error: null, // 액션 성공 시 에러 초기화
            };
        case ActionTypes.ACTION_ERROR:
            return {
                ...state,
                actionLoading: false,
                error: action.payload.error, // 에러 객체 또는 메시지 저장
                modalMessage: action.payload.modalMessage, // 에러 메시지를 모달에 표시
            };
        case ActionTypes.SET_MODAL_MESSAGE:
            return {
                ...state,
                modalMessage: action.payload,
            };
        case ActionTypes.CLEAR_MODAL_MESSAGE:
            return {
                ...state,
                modalMessage: '',
            };
        default:
            return state;
    }
}

// --- Reducer 관련 정의 끝 ---

function TeamRankingList() {
    const [state, dispatch] = useReducer(rankingReducer, initialState);
    const { rankings, allTeams, loading, actionLoading, error, modalMessage } = state;

    const { user } = useContext(AuthContext);
    const isAdmin = user && user.role === 'ADMIN';

    const currentYear = new Date().getFullYear();

    // 팀 이름으로 로고 URL을 찾는 함수
    const getTeamLogoUrl = useCallback((teamName) => {
        const team = allTeams.find(t => t.name === teamName);
        return team ? team.logoUrl : 'https://placehold.co/24x24/CCCCCC/000000?text=L'; // 기본 로고 또는 플레이스홀더
    }, [allTeams]);

    // 이미지 로드 실패 시 대체 이미지 설정
    const handleImageError = useCallback((e) => {
        e.target.onerror = null; // 무한 루프 방지
        e.target.src = 'https://placehold.co/24x24/CCCCCC/000000?text=L'; // 대체 이미지
    }, []);

    // 순위 데이터를 불러오는 주 함수 (관리자 액션 후에도 재사용)
    const fetchRankings = useCallback(async () => {
        dispatch({ type: ActionTypes.FETCH_START }); // 데이터 로딩 시작 액션 디스패치

        try {
            // 팀 로고 매핑을 위해 모든 팀 정보를 먼저 가져옵니다.
            const teamResponse = await teamApi.getAllTeams();
            // 특정 시즌의 팀 순위 정보를 가져옵니다.
            const rankingResponse = await teamRankingApi.getAllTeamRankings(currentYear);

            // 성공 시, 순위와 모든 팀 데이터를 페이로드로 전달
            dispatch({
                type: ActionTypes.FETCH_SUCCESS,
                payload: {
                    rankings: rankingResponse.data,
                    allTeams: teamResponse.data,
                },
            });
        } catch (err) {
            console.error('팀 순위를 불러오는 데 실패했습니다:', err);
            // 'error' 필드에 직접 에러 메시지를 할당
            dispatch({
                type: ActionTypes.FETCH_ERROR,
                payload: {
                    error: '팀 순위를 불러오는 데 실패했습니다.', // 여기에 실제 에러 메시지
                    modalMessage: '팀 순위를 불러오는 중 오류가 발생했습니다.', // 모달에 표시될 메시지
                },
            });
        }
    }, [currentYear]);

    useEffect(() => {
        fetchRankings();
    }, [fetchRankings]); // fetchRankings 함수가 변경될 때마다 실행 (의존성 배열 최적화)

    const handleModalClose = useCallback(() => {
        dispatch({ type: ActionTypes.CLEAR_MODAL_MESSAGE }); // 모달 메시지 초기화 액션 디스패치
    }, []);

    // 관리자: KBO 웹사이트에서 크롤링하여 순위 업데이트 (백엔드의 updateRankingsFromCrawl 호출)
    const handleCrawlAndUpdateRankings = useCallback(async () => {
        if (!isAdmin) {
            dispatch({ type: ActionTypes.SET_MODAL_MESSAGE, payload: '권한이 없습니다.' });
            return;
        }
        dispatch({ type: ActionTypes.ACTION_START }); // 액션 시작 상태
        try {
            const response = await teamRankingApi.crawlAndUpdateRankings(currentYear); // <-- 백엔드 API 호출
            dispatch({
                type: ActionTypes.ACTION_SUCCESS,
                payload: {
                    rankings: response.data, // 백엔드에서 반환된 최신 순위 데이터
                    modalMessage: 'KBO 웹사이트에서 순위가 성공적으로 크롤링 및 업데이트되었습니다!',
                },
            });
        } catch (err) {
            console.error('크롤링 및 순위 업데이트 실패:', err);
            dispatch({
                type: ActionTypes.ACTION_ERROR,
                payload: {
                    error: `크롤링 중 오류 발생: ${err.response?.data?.message || err.message}`, // 여기에 실제 에러 메시지
                    modalMessage: `크롤링 중 오류가 발생했습니다: ${err.response?.data?.message || err.message}`,
                },
            });
        }
    }, [isAdmin, currentYear]);


    // 로딩 중이거나 에러 발생 시 UI (초기 로딩 시)
    if (loading) {
        return <div className="text-center py-4 text-secondary">팀 순위를 불러오는 중...</div>;
    }

    // 'error' 상태가 있을 때 직접 화면에 에러 메시지를 표시하여 'error' 변수 사용
    if (error) {
        return <div className="text-center py-4 text-danger fw-bold">오류: {error}</div>;
    }

    return (
        <div className="container my-2">
            <h3 className="mb-3 text-center">⚾ {currentYear} 시즌 팀 순위</h3>

            {/* 관리자 권한이 있을 때만 버튼을 표시 */}
            {isAdmin && (
                <div className="d-flex justify-content-end mb-4"> {/* justify-content-center를 justify-content-end로 변경 */}
                    <button
                        className="btn btn-secondary btn-sm" // btn-warning를 btn-secondary로 변경
                        onClick={handleCrawlAndUpdateRankings}
                        disabled={actionLoading} // 액션 진행 중에는 버튼 비활성화
                    >
                        {actionLoading ? '크롤링 중...' : 'KBO 크롤링으로 순위 업데이트'}
                    </button>
                </div>
            )}

            {rankings.length === 0 ? (
                <div className="alert alert-info text-center my-4" role="alert">
                    현재 시즌의 팀 순위 정보가 없습니다. 관리자에게 문의하거나 순위를 계산해 주세요.
                </div>
            ) : (
                <div className="table-responsive">
                    <table className="table align-middle table-hover" style={{ borderCollapse: 'collapse', width: '100%' }}>
                        <thead style={{ backgroundColor: '#f8f9fa', borderBottom: '1px solid #e0e0e0' }}>
                            <tr>
                                <th scope="col" className="text-start ps-3" style={{ padding: '12px 4px', width: '30%' }}>순위 팀</th>
                                <th scope="col" className="text-center" style={{ padding: '12px 4px', width: '8%' }}>승</th>
                                <th scope="col" className="text-center" style={{ padding: '12px 4px', width: '8%' }}>무</th>
                                <th scope="col" className="text-center" style={{ padding: '12px 4px', width: '8%' }}>패</th>
                                <th scope="col" className="text-center" style={{ padding: '12px 4px', width: '12%' }}>승률</th>
                                <th scope="col" className="text-center" style={{ padding: '12px 4px', width: '12%' }}>게임차</th>
                            </tr>
                        </thead>
                        <tbody>
                            {rankings.map((ranking) => (
                                <tr key={ranking.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                                    <td className="text-start ps-3" style={{ padding: '10px 4px' }}>
                                        <div className="d-flex align-items-center">
                                            <span className="fw-bold me-2" style={{ width: '20px', textAlign: 'center' }}>{ranking.currentRank}</span>
                                            <img
                                                src={getTeamLogoUrl(ranking.team.name)}
                                                alt={`${ranking.team.name} 로고`}
                                                style={{ width: '28px', height: '28px', objectFit: 'contain', marginRight: '10px', borderRadius: '50%', border: '1px solid #eee' }}
                                                onError={handleImageError}
                                            />
                                            <span className="fw-medium">{ranking.team.name}</span>
                                        </div>
                                    </td>
                                    <td className="text-center" style={{ padding: '10px 4px' }}>{ranking.wins}</td>
                                    <td className="text-center" style={{ padding: '10px 4px' }}>{ranking.draws}</td>
                                    <td className="text-center" style={{ padding: '10px 4px' }}>{ranking.losses}</td>
                                    <td className="text-center fw-bold" style={{ padding: '10px 4px', color: '#3B82F6' }}>
                                        {/* winRate가 NaN이거나 Infinity일 경우 처리 */}
                                        {isNaN(ranking.winRate) || !isFinite(ranking.winRate) ? '0.000' : ranking.winRate.toFixed(3)}
                                    </td>
                                    <td className="text-center" style={{ padding: '10px 4px' }}>
                                        {/* gamesBehind가 0.0일 경우 '-'로 표시, 아니면 소수점 첫째 자리까지 표시 */}
                                        {ranking.gamesBehind === 0.0 ? '-' : ranking.gamesBehind.toFixed(1)}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {isAdmin && (
                <div className="text-center mt-4">
                    <Link to="/admin/rankings" className="btn btn-primary">팀 순위 관리</Link>
                </div>
            )}

            {/* modalMessage가 있을 때만 모달 렌더링 */}
            <Modal message={modalMessage} onClose={handleModalClose} />
        </div>
    );
}

export default TeamRankingList;
