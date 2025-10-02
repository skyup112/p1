import React from 'react';
import { getTeamLogoUrl, handleImageError } from './GameDetail'; // Helper functions assumed to be available or imported from GameDetail

const LineupSection = ({
    teamType, // 'home' or 'away'
    teamInfo, // game.homeTeam or game.opponentTeam
    allAvailableTeams,
    expectedLineup, // homeExpectedLineup or awayExpectedLineup (for view mode)
    editedLineupPlayers, // editedHomeLineupPlayers or editedAwayLineupPlayers (for edit mode)
    isLineupEditMode,
    isAdmin,
    dispatch,
    handleLineupPlayerChange,
    handleAddPlayer,
    handleRemovePlayer,
    handleLineupUpdateSubmit,
}) => {
    const teamName = teamInfo ? teamInfo.name : (teamType === 'home' ? '홈 팀' : '원정 팀');
    const teamLogo = teamInfo ? getTeamLogoUrl(teamInfo.id, allAvailableTeams) : 'https://placehold.co/50x50/CCCCCC/000000?text=Logo';
    const teamColorClass = teamType === 'home' ? 'text-primary' : 'text-danger';

    // Helper to get players for view mode (use expectedLineup if available, otherwise an empty array)
    const viewPlayers = expectedLineup && expectedLineup.players ? expectedLineup.players : [];

    // Helper to get players for edit mode (use editedLineupPlayers)
    const editPlayers = editedLineupPlayers || [];

    const handleCancelEdit = () => {
        // 편집 모드 취소 시, 기존의 expectedLineup 데이터로 editedLineupPlayers를 복원합니다.
        // null orderNumber를 가장 뒤로 보내기 위한 안전한 정렬 로직 추가
        const playersToReset = expectedLineup && expectedLineup.players 
            ? [...expectedLineup.players].sort((a, b) => {
                const orderA = a.orderNumber === null ? Number.MAX_SAFE_INTEGER : a.orderNumber;
                const orderB = b.orderNumber === null ? Number.MAX_SAFE_INTEGER : b.orderNumber;
                return orderA - orderB;
            }) 
            : [];
        
        if (teamType === 'home') {
            dispatch({ type: 'SET_HOME_LINEUP_EDIT_MODE', payload: false });
            dispatch({ type: 'SET_EDITED_HOME_PLAYERS', payload: playersToReset });
        } else {
            dispatch({ type: 'SET_AWAY_LINEUP_EDIT_MODE', payload: false });
            dispatch({ type: 'SET_EDITED_AWAY_PLAYERS', payload: playersToReset });
        }
    };

    const handleSetEditMode = (mode) => {
        if (teamType === 'home') {
            dispatch({ type: 'SET_HOME_LINEUP_EDIT_MODE', payload: mode });
        } else {
            dispatch({ type: 'SET_AWAY_LINEUP_EDIT_MODE', payload: mode });
        }
    };

    return (
        <div className="col-md-6 mb-5">
            <h4 className={`text-center mb-3 ${teamColorClass}`}>
                <img src={teamLogo} alt={`${teamName} 로고`} style={{ height: '40px', marginRight: '10px', verticalAlign: 'middle' }} onError={handleImageError} />
                {teamName} 예상 선발 라인업
            </h4>
            <ul className="list-group list-group-flush shadow-sm">
                {/* 라인업 리스트 렌더링 (편집 모드와 보기 모드 분리) */}
                {isLineupEditMode ? (
                    // Edit Mode Rendering
                    editPlayers.length > 0 ? (
                        editPlayers.map((player, index) => (
                            <li key={player.id || `${teamType}-new-${index}`} className="list-group-item d-flex justify-content-between align-items-center py-2 px-3">
                                <div className="d-flex w-100 align-items-center">
                                    <input
                                        type="number"
                                        value={player.orderNumber || ''}
                                        onChange={(e) => handleLineupPlayerChange(teamType, index, 'orderNumber', e.target.value)}
                                        className="form-control form-control-sm me-2"
                                        style={{ width: '60px' }}
                                    />
                                    <input
                                        type="text"
                                        value={player.playerName || ''}
                                        onChange={(e) => handleLineupPlayerChange(teamType, index, 'playerName', e.target.value)}
                                        placeholder="선수 이름"
                                        className="form-control form-control-sm me-2 flex-grow-1"
                                    />
                                    <input
                                        type="text"
                                        value={player.position || ''}
                                        onChange={(e) => handleLineupPlayerChange(teamType, index, 'position', e.target.value)}
                                        placeholder="포지션 (예: SP, RF)"
                                        className="form-control form-control-sm me-2"
                                        style={{ width: '100px' }}
                                    />
                                    <button
                                        className="btn btn-danger btn-sm"
                                        onClick={() => handleRemovePlayer(teamType, index)}
                                    >
                                        삭제
                                    </button>
                                </div>
                            </li>
                        ))
                    ) : (
                        <li className="list-group-item text-muted text-center py-3">
                            라인업을 추가하려면 '선수 추가'를 클릭하세요.
                        </li>
                    )
                ) : (
                    // View Mode Rendering
                    viewPlayers.length > 0 ? (
                        viewPlayers.map((player, index) => (
                            <li key={player.id || index} className="list-group-item d-flex justify-content-between align-items-center py-2 px-3">
                                <span>
                                    <strong>{player.orderNumber}.</strong> {player.playerName} ({player.position})
                                </span>
                            </li>
                        ))
                    ) : (
                        <li className="list-group-item text-muted text-center py-3">
                            아직 {teamName} 예상 라인업이 없습니다.
                        </li>
                    )
                )}

                {/* 선수 추가 버튼 (편집 모드일 때만 표시) */}
                {isAdmin && isLineupEditMode && (
                    <li className="list-group-item py-2 px-3">
                        <button
                            className="btn btn-outline-success btn-sm w-100"
                            onClick={() => handleAddPlayer(teamType)}
                        >
                            선수 추가
                        </button>
                    </li>
                )}
            </ul>
            
            {/* 라인업 수정/저장/취소 버튼 */}
            {isAdmin && (
                <div className="mt-3 d-flex justify-content-end gap-2">
                    {isLineupEditMode ? (
                        <>
                            <button
                                className="btn btn-success rounded-pill px-4 py-2 fw-bold shadow-sm"
                                onClick={() => handleLineupUpdateSubmit(teamType)}
                            >
                                라인업 저장
                            </button>
                            <button
                                className="btn btn-secondary rounded-pill px-4 py-2 fw-bold shadow-sm"
                                onClick={handleCancelEdit}
                            >
                                취소
                            </button>
                        </>
                    ) : (
                        <button
                            className="btn btn-primary btn-sm"
                            onClick={() => handleSetEditMode(true)}
                        >
                            라인업 수정
                        </button>
                    )}
                </div>
            )}
        </div>
    );
};

export default LineupSection;
