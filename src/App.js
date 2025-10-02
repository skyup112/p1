import React, { useContext, useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useNavigate } from 'react-router-dom';
import GameList from './components/GameList';
import GameDetail from './components/GameDetail';
import AdminMembers from './components/AdminMembers';
import AdminTeams from './components/AdminTeams';
import TeamRankingList from './components/TeamRankingList';
import AdminTeamRankings from './components/AdminTeamRankings';
import LoginForm from './components/LoginForm';
import RegisterForm from './components/RegisterForm';
import MemberProfile from './components/MemberProfile';
import Modal from './components/Modal';
import { AuthProvider, AuthContext } from './context/AuthContext';
import GameForm from './components/GameForm';
import Home from './components/Home'; // Home 컴포넌트 임포트

// ProtectedRoute 컴포넌트: 인증 및 역할 기반 접근 제어를 위한 래퍼
const ProtectedRoute = ({ children, adminOnly }) => {
    const { isAuthenticated, loading, user } = useContext(AuthContext);
    const navigate = useNavigate();
    const [modalMessage, setModalMessage] = useState('');

    useEffect(() => {
        if (!loading) {
            if (!isAuthenticated) {
                console.log("ProtectedRoute: Not authenticated. Redirecting to /login (via useEffect)");
                navigate('/login');
            } else if (adminOnly && (!user || user.role !== 'ADMIN')) {
                console.log("ProtectedRoute: Authenticated but not ADMIN. Redirecting to / (via useEffect)");
                setModalMessage('관리자 권한이 없습니다.');
                setTimeout(() => {
                    navigate('/');
                }, 0);
            }
        }
    }, [isAuthenticated, user, adminOnly, navigate, loading]);

    if (loading) {
        console.log("ProtectedRoute: Authentication info is loading...");
        return <div className="text-center py-4 text-secondary">인증 정보 확인 중...</div>;
    }

    if (!isAuthenticated || (adminOnly && (!user || user.role !== 'ADMIN'))) {
        return null;
    }

    const handleModalClose = () => {
        setModalMessage('');
    };

    return (
        <>
            {children}
            <Modal message={modalMessage} onClose={handleModalClose} />
        </>
    );
};

// 메인 App 컴포넌트
function App() {
    const { isAuthenticated, logout, user } = useContext(AuthContext);
    const navigate = useNavigate();
    const [modalMessage, setModalMessage] = useState('');

    const handleLogoutClick = async () => {
        try {
            await logout();
            setModalMessage('로그아웃되었습니다.');
            setTimeout(() => navigate('/login'), 1500);
        } catch (error) {
            console.error('로그아웃 실패:', error);
            setModalMessage('로그아웃에 실패했습니다.');
        }
    };

    const handleModalClose = () => {
        setModalMessage('');
        if (modalMessage.includes('로그아웃되었습니다.')) {
            navigate('/login');
        }
    };

    const handleRegisterGame = async (newGameData) => {
        try {
            const { gameScheduleApi } = await import('./services/apiService');

            const gameDTO = {
                gameDate: newGameData.gameDate,
                location: newGameData.location,
                homeScore: newGameData.homeScore,
                awayScore: newGameData.awayScore,
                opponentTeam: newGameData.opponentTeam,
            };

            await gameScheduleApi.createGame(gameDTO);
            setModalMessage('경기가 성공적으로 등록되었습니다.');
            navigate('/');
        } catch (error) {
            console.error('경기 등록 실패:', error);
            setModalMessage(`경기 등록에 실패했습니다: ${error.response?.data?.message || error.message}`);
        }
    };

    return (
        <div className="container-fluid p-0 bg-light min-vh-100">
            {/* Header (Top Dark Bar) - giantsclub.com 디자인 참조 */}
            <header className="container bg-dark text-white py-1 shadow-lg">
                <div className="d-flex justify-content-end align-items-center px-4">
                    <ul className="nav">
                        {!isAuthenticated ? (
                            <>
                                <li className="nav-item">
                                    <Link to="/login" className="nav-link text-white-50 small">로그인</Link>
                                </li>
                                <li className="nav-item">
                                    <Link to="/register" className="nav-link text-white-50 small">회원가입</Link>
                                </li>
                            </>
                        ) : (
                            <>
                                {/* 로그인된 사용자 메뉴 (내 정보 관리, 닉네임, 로그아웃) */}
                                <li className="nav-item">
                                    <Link to="/my-profile" className="nav-link text-white-50 small">내 정보 관리</Link>
                                </li>
                                <li className="nav-item">
                                    <span className="nav-link text-white-50 small">{user.nickname}님 환영합니다</span>
                                </li>
                                <li className="nav-item">
                                    <button onClick={handleLogoutClick} className="btn nav-link text-white-50 small">로그아웃</button>
                                </li>
                            </>
                        )}
                    </ul>
                </div>

                {/* 메인 로고 및 네비게이션 */}
                <div className="container bg-lotte-giants-red text-white py-1 border-top border-bottom border-secondary" style={{ backgroundColor: '#CD112B' }}>
                    {/* justify-content-between 제거: 모든 요소를 왼쪽으로 정렬 */}
                    <div className="d-flex align-items-center px-4">
                        {/* justify-content-between 및 width: '100%' 제거: 로고와 메뉴가 자연스럽게 이어지도록 */}
                        <div className="d-flex align-items-center">
                            <Link to="/" className="navbar-brand text-white fw-bold fs-2 me-5" >
                                <img
                                    src="./main.png"
                                    alt="Giants Logo"
                                    style={{ height: '84px', width: '200px', objectFit: 'contain' }}
                                />
                            </Link>
                            {/* 메뉴 아이템들을 묶기 위한 div */}
                            <div className="d-flex align-items-center">
                                {/* 경기 일정 링크 (GameList로 이동) */}
                                <Link to="/games" className="nav-link text-white fw-semibold px-4 py-3 ms-6">경기 일정</Link>
                                {/* 팀 순위 링크 (TeamRankingList로 이동) */}
                                <Link to="/rankings" className="nav-link text-white fw-semibold px-4 py-3 ms-6">팀 순위</Link>

                                {/* 관리자 메뉴 */}
                                {isAuthenticated && user && user.role === 'ADMIN' && (
                                    <>
                                        <Link to="/admin/members" className="nav-link text-white fw-semibold px-3 py-2 ms-5">회원 관리</Link>
                                        <Link to="/admin/teams" className="nav-link text-white fw-semibold px-3 py-2 ms-5">팀 관리</Link>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </header>

            <main className="py-3 px-3">
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/games" element={<GameList />} />
                    <Route path="/games/:gameId" element={<GameDetail />} />
                    <Route path="/login" element={<LoginForm />} />
                    <Route path="/register" element={<RegisterForm />} />
                    <Route path="/rankings" element={<TeamRankingList />} />

                    <Route
                        path="/admin/members"
                        element={
                            <ProtectedRoute adminOnly={true}>
                                <AdminMembers />
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/admin/teams"
                        element={
                            <ProtectedRoute adminOnly={true}>
                                <AdminTeams />
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/games/register"
                        element={
                            <ProtectedRoute adminOnly={true}>
                                <GameForm
                                    onSubmit={handleRegisterGame}
                                    buttonText="경기 등록"
                                    title="새 경기 등록"
                                />
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/admin/rankings"
                        element={
                            <ProtectedRoute adminOnly={true}>
                                <AdminTeamRankings />
                            </ProtectedRoute>
                        }
                    />
                    <Route
                        path="/my-profile"
                        element={
                            <ProtectedRoute>
                                <MemberProfile />
                            </ProtectedRoute>
                        }
                    />
                    <Route path="*" element={<h2 className="text-center text-secondary mt-5">페이지를 찾을 수 없습니다 (404)</h2>} />
                </Routes>
            </main>
            <Modal message={modalMessage} onClose={handleModalClose} />
        </div>
    );
}

function AppWrapper() {
    return (
        <Router>
            <AuthProvider>
                <App />
            </AuthProvider>
        </Router>
    );
}

export default AppWrapper;