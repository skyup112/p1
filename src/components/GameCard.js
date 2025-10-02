import React, { useCallback } from 'react';
import { Link } from 'react-router-dom';

function GameCard({ game, allTeams, type = 'default' }) {
    // Helper to get team logo URL by team ID (more robust than by name)
    // This function assumes allTeams is an array of objects like { id: 1, name: "Team A", logoUrl: "..." }
    const getTeamLogoUrl = useCallback((teamId) => {
        const team = allTeams.find(t => t.id === teamId);
        // Fallback to a placeholder if team or logoUrl is not found
        return team && team.logoUrl ? team.logoUrl : 'https://placehold.co/100x100/CCCCCC/000000?text=Logo';
    }, [allTeams]);

    // Image error handler
    const handleImageError = useCallback((e) => {
        e.target.onerror = null;
        e.target.src = 'https://placehold.co/80x80/CCCCCC/000000?text=Logo';
    }, []);

    // If no game data, render an empty card
    if (!game) {
        const emptyCardMinHeight = type === 'main' ? '200px' : (type === 'calendar' ? '80px' : '100px');
        return (
            <div className="text-center h-100 d-flex align-items-center justify-content-center p-1" style={{ minHeight: emptyCardMinHeight }}>
                <p className="text-muted mb-0 small">경기 없음</p>
            </div>
        );
    }

    // --- Dynamic Team Assignment ---
    // Use the actual home and opponent team objects from the 'game' prop
    // These should be objects like {id: 1, name: "LG 트윈스", logoUrl: "..."}
    const homeTeam = game.homeTeam;
    const opponentTeam = game.opponentTeam;

    const isFinished = game.status === 'FINISHED';
    const isScheduled = game.status === 'SCHEDULED'; // Corrected typo from SCHEDULEED
    const isInProgress = game.status === 'IN_PROGRESS';
    const isCanceled = game.status === 'CANCELED';

    let cardClass = "card shadow-sm border rounded-lg p-3 text-center h-100 d-flex flex-column justify-content-between";
    let logoSize = 60;
    let vsFontSize = "1.5rem";
    let scoreTimeFontSize = "1.8rem";
    let statusLocationFontSize = "0.9rem";
    let teamNameFontSize = "0.9rem";

    if (type === 'main') {
        cardClass = "card shadow-lg border-2 rounded-lg p-4 text-center h-100 d-flex flex-column justify-content-between";
        logoSize = 80;
        vsFontSize = "2rem";
        scoreTimeFontSize = "2.5rem";
        statusLocationFontSize = "1rem";
        teamNameFontSize = "1rem";
    } else if (type === 'calendar') {
        cardClass = "p-0 text-center d-flex flex-column justify-content-start";
        logoSize = 35;
        vsFontSize = "1rem";
        scoreTimeFontSize = "1.1rem";
        statusLocationFontSize = "0.7rem";
        teamNameFontSize = "0.7rem"; // This will now apply to calendar view team names
    } else if (type === 'list') {
        cardClass = "card shadow-sm border rounded-lg p-2 text-center d-flex flex-column justify-content-between";
        logoSize = 40;
        vsFontSize = "1.1rem";
        scoreTimeFontSize = "1.3rem";
        statusLocationFontSize = "0.75rem";
        teamNameFontSize = "0.8rem";
    }

    const currentLogoContainerStyle = {
        width: `${logoSize}px`,
        height: `${logoSize}px`,
        borderRadius: '50%',
        overflow: 'hidden',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        border: '1px solid #e0e0e0',
        backgroundColor: '#f8f9fa'
    };

    // --- Win/Loss Status for Display ---
    let homeWinLossStatusText = '';
    let opponentWinLossStatusText = '';

    if (isFinished) {
        if (game.homeScore > game.awayScore) {
            homeWinLossStatusText = ' (승)';
            opponentWinLossStatusText = ' (패)';
        } else if (game.homeScore < game.awayScore) {
            homeWinLossStatusText = ' (패)';
            opponentWinLossStatusText = ' (승)';
        } else {
            homeWinLossStatusText = ' (무)';
            opponentWinLossStatusText = ' (무)';
        }
    }
    // --- End Win/Loss Status for Display ---

    const gameDate = game.gameDate ? new Date(game.gameDate) : null;
    const formattedDate = gameDate ? gameDate.toLocaleDateString('ko-KR', { month: '2-digit', day: '2-digit', weekday: 'short' }) : '';
    
    const gameTime = gameDate ? gameDate.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false }) : '';
    const displayTime = gameTime !== '00:00' ? gameTime : ''; // Only display time if it's not '00:00'

    let statusTextColorClass = '';
    let statusText = '';
    if (isFinished) {
        statusTextColorClass = 'text-dark';
        statusText = '경기 종료';
    } else if (isScheduled) {
        statusTextColorClass = 'text-secondary';
        statusText = '경기 예정';
    } else if (isInProgress) {
        statusTextColorClass = 'text-success';
        statusText = '경기 진행 중';
    } else if (isCanceled) {
        statusTextColorClass = 'text-danger';
        statusText = '경기 취소';
    }

    const pCombinedClassName = `fw-bold mt-2 mb-0 ${statusTextColorClass}`;

    const vsSpanStyle = { fontSize: vsFontSize };
    const scoreSpanStyle = { fontSize: scoreTimeFontSize };

    return (
        // Link only if game.id exists, otherwise it will try to navigate to /games/null
        <Link to={game.id ? `/games/${game.id}` : '#'} className="text-decoration-none text-dark hover-text-primary d-block h-100">
            <div className={cardClass}>
                <div className="d-flex flex-column p-0">
                    <div className="d-flex flex-column justify-content-center align-items-center mb-2">
                        <span className="text-secondary fw-semibold" style={{ fontSize: statusLocationFontSize }}>
                            {formattedDate} {displayTime}
                        </span>
                        <span className="text-secondary fw-semibold" style={{ fontSize: statusLocationFontSize }}>
                            [{game.location}]
                        </span>
                    </div>

                    <div className="d-flex justify-content-center align-items-center mb-0">
                        {homeTeam && ( // Only render if homeTeam object exists
                            <div className="d-flex flex-column align-items-center" style={{ margin: type === 'calendar' ? '0 5px' : '0 10px' }}>
                                <div className="logo-container mb-1" style={currentLogoContainerStyle}>
                                    <img
                                        src={getTeamLogoUrl(homeTeam.id)}
                                        alt={`${homeTeam.name} 로고`}
                                        className="img-fluid"
                                        style={{ objectFit: 'contain' }}
                                        onError={handleImageError}
                                    />
                                </div>
                                {/* ⭐ 변경: 달력 뷰가 아닐 때만 팀 이름 표시 ⭐ */}
                                {type !== 'calendar' && (
                                    <span
                                        className="fw-bold text-dark"
                                        style={{
                                            fontSize: teamNameFontSize,
                                            whiteSpace: 'nowrap',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            display: 'block',
                                            width: '100%'
                                        }}
                                    >
                                        {homeTeam.name}
                                    </span>
                                )}
                                {/* ⭐ 변경: 경기 종료 시 항상 승패 표시 ⭐ */}
                                {isFinished && homeWinLossStatusText && (
                                    <span className={`fw-normal ${homeWinLossStatusText === ' (승)' ? 'text-success' : homeWinLossStatusText === ' (패)' ? 'text-danger' : 'text-secondary'}`} style={{ fontSize: teamNameFontSize }}>
                                        {homeWinLossStatusText}
                                    </span>
                                )}
                            </div>
                        )}

                        <div className="d-flex justify-content-center align-items-center" style={{ minWidth: type === 'calendar' ? '40px' : '80px', margin: type === 'calendar' ? '0 5px' : '0 10px' }}>
                            {isFinished ? (
                                <span className="fw-bold text-dark" style={scoreSpanStyle}>
                                    {game.homeScore}:{game.awayScore}
                                </span>
                            ) : (
                                <span className="fw-bold text-muted" style={vsSpanStyle}>VS</span>
                            )}
                        </div>

                        {opponentTeam && ( // Only render if opponentTeam object exists
                            <div className="d-flex flex-column align-items-center" style={{ margin: type === 'calendar' ? '0 5px' : '0 10px' }}>
                                <div className="logo-container mb-1" style={currentLogoContainerStyle}>
                                    <img
                                        src={getTeamLogoUrl(opponentTeam.id)}
                                        alt={`${opponentTeam.name} 로고`}
                                        className="img-fluid"
                                        style={{ objectFit: 'contain' }}
                                        onError={handleImageError}
                                    />
                                </div>
                                {/* ⭐ 변경: 달력 뷰가 아닐 때만 팀 이름 표시 ⭐ */}
                                {type !== 'calendar' && (
                                    <span
                                        className="fw-bold text-dark"
                                        style={{
                                            fontSize: teamNameFontSize,
                                            whiteSpace: 'nowrap',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            display: 'block',
                                            width: '100%'
                                        }}
                                    >
                                        {opponentTeam.name}
                                    </span>
                                )}
                                {/* ⭐ 변경: 경기 종료 시 항상 승패 표시 ⭐ */}
                                {isFinished && opponentWinLossStatusText && (
                                    <span className={`fw-normal ${opponentWinLossStatusText === ' (승)' ? 'text-success' : opponentWinLossStatusText === ' (패)' ? 'text-danger' : 'text-secondary'}`} style={{ fontSize: teamNameFontSize }}>
                                        {opponentWinLossStatusText}
                                    </span>
                                )}
                            </div>
                        )}
                    </div>
                    <p className={pCombinedClassName} style={{ fontSize: statusLocationFontSize }}>
                        {statusText}
                    </p>
                </div>
            </div>
        </Link>
    );
}
export default GameCard;
