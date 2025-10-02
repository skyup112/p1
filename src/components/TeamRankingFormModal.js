import React, { useState, useEffect, useCallback } from 'react';
import Modal from './Modal'; // 기존의 범용 모달 컴포넌트 재사용

/**
 * TeamRankingFormModal 컴포넌트
 * 팀 순위 정보를 생성하거나 수정하기 위한 모달 폼입니다.
 *
 * @param {object} props - 컴포넌트 속성
 * @param {string} props.title - 모달의 제목
 * @param {string} props.buttonText - 제출 버튼의 텍스트
 * @param {object} [props.initialData] - 폼의 초기 데이터 (수정 시 사용). null 또는 빈 객체면 생성 모드.
 * @param {function} props.onSubmit - 폼 제출 시 호출될 콜백 함수. (formData) => Promise<void>
 * @param {function} props.onClose - 모달 닫기 버튼 클릭 시 호출될 콜백 함수.
 * @param {Array<object>} props.allTeams - 모든 팀 목록 (TeamDTO 형태: {id, name, logoUrl})
 */
function TeamRankingFormModal({ title, buttonText, initialData, onSubmit, onClose, allTeams }) {
    // 승률 계산 함수 (TeamRankingList에서 사용했던 것과 동일)
    // useCallback을 사용하여 함수가 불필요하게 재생성되는 것을 방지합니다.
    const calculateWinRate = useCallback((wins, losses) => {
        // 야구 승률 계산 방식: 승 / (승 + 패). 무승부는 승률 계산에 포함하지 않음.
        const totalDecisionGames = wins + losses;
        if (totalDecisionGames === 0) {
            return 0.0; // 0으로 나누는 것을 방지
        }
        return wins / totalDecisionGames;
    }, []);

    const [formData, setFormData] = useState(() => {
        const defaultData = {
            team: null, // TeamDTO 객체
            seasonYear: new Date().getFullYear(),
            wins: 0,
            losses: 0,
            draws: 0,
            winRate: 0.0, // 초기값 설정
            currentRank: 0,
            gamesBehind: 0.0, // 초기값 설정
        };
        // initialData가 있으면 해당 데이터를 사용하고, winRate는 다시 계산하여 적용
        if (initialData) {
            return {
                ...initialData,
                winRate: calculateWinRate(initialData.wins, initialData.losses)
            };
        }
        return defaultData;
    });

    const [selectedTeamId, setSelectedTeamId] = useState(initialData?.team?.id || '');

    // initialData 또는 allTeams가 변경될 때마다 formData 업데이트
    useEffect(() => {
        if (initialData) {
            setFormData({
                ...initialData,
                team: initialData.team || null,
                // 초기 데이터 로드 시에도 승률을 계산하여 설정
                winRate: calculateWinRate(initialData.wins, initialData.losses),
            });
            setSelectedTeamId(initialData.team?.id || '');
        } else {
            // 새 팀 순위 등록 시 초기화
            setFormData({
                team: null,
                seasonYear: new Date().getFullYear(),
                wins: 0,
                losses: 0,
                draws: 0,
                winRate: 0.0,
                currentRank: 0,
                gamesBehind: 0.0,
            });
            setSelectedTeamId('');
        }
    }, [initialData, allTeams, calculateWinRate]);

    const handleChange = (e) => {
        const { id, value } = e.target;

        setFormData(prevData => {
            let updatedData = { ...prevData };

            if (id === 'team') { // 팀 선택 드롭다운
                const selectedTeam = allTeams.find(team => team.id === parseInt(value));
                setSelectedTeamId(value);
                updatedData.team = selectedTeam ? { id: selectedTeam.id, name: selectedTeam.name, logoUrl: selectedTeam.logoUrl } : null;
            } else {
                // 숫자 필드는 parseInt/parseFloat로 변환, 다른 필드는 그대로
                // gamesBehind를 parseFloat로 처리
                const numValue = (id === 'wins' || id === 'losses' || id === 'draws' ||
                                  id === 'seasonYear' || id === 'currentRank')
                                  ? parseInt(value, 10) || 0 // 정수
                                  : (id === 'gamesBehind')
                                    ? parseFloat(value) || 0.0 // 실수 (수동 수정 허용)
                                    : value;
                updatedData[id] = numValue;
            }

            // 승, 패 중 하나라도 변경되면 승률 재계산 (draws는 승률에 영향 없음)
            if (id === 'wins' || id === 'losses') {
                const newWins = updatedData.wins;
                const newLosses = updatedData.losses;
                updatedData.winRate = calculateWinRate(newWins, newLosses);
            }

            return updatedData;
        });
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        // gamesBehind 값을 formData에 포함하여 onSubmit 콜백으로 전달
        onSubmit(formData);
    };

    // 폼 유효성 검사 (간단하게 필수 필드만 확인)
    const isFormValid = formData.team && formData.seasonYear > 0 &&
                        formData.wins >= 0 && formData.losses >= 0 && formData.draws >= 0;


    return (
        <Modal title={title} onClose={onClose}>
            <form onSubmit={handleSubmit}>
                <div className="mb-3">
                    <label htmlFor="team" className="form-label fw-bold">팀:</label>
                    <select
                        id="team"
                        value={selectedTeamId}
                        onChange={handleChange}
                        required
                        className="form-select"
                        disabled={initialData && initialData.id} // 수정 시 팀 변경 불가
                    >
                        <option value="">팀을 선택하세요</option>
                        {allTeams.map(team => (
                            <option key={team.id} value={team.id}>
                                {team.name}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="mb-3">
                    <label htmlFor="seasonYear" className="form-label fw-bold">시즌 연도:</label>
                    <input
                        type="number"
                        id="seasonYear"
                        value={formData.seasonYear}
                        onChange={handleChange}
                        required
                        className="form-control"
                        disabled={initialData && initialData.id} // 보통 수정 시 시즌 연도도 변경 불가
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="wins" className="form-label fw-bold">승:</label>
                    <input
                        type="number"
                        id="wins"
                        value={formData.wins}
                        onChange={handleChange}
                        required
                        className="form-control"
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="losses" className="form-label fw-bold">패:</label>
                    <input
                        type="number"
                        id="losses"
                        value={formData.losses}
                        onChange={handleChange}
                        required
                        className="form-control"
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="draws" className="form-label fw-bold">무:</label>
                    <input
                        type="number"
                        id="draws"
                        value={formData.draws}
                        onChange={handleChange}
                        required
                        className="form-control"
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="winRate" className="form-label fw-bold">승률:</label>
                    <input
                        type="text" // 숫자가 아니라 텍스트 타입으로 변경하여 소수점 처리 용이하게 함
                        id="winRate"
                        // isNaN 또는 !isFinite 체크를 통해 유효하지 않은 숫자일 경우 '0.000' 표시
                        value={isNaN(formData.winRate) || !isFinite(formData.winRate) ? '0.000' : formData.winRate.toFixed(3)}
                        readOnly // 승률은 자동으로 계산되므로 읽기 전용으로 설정
                        className="form-control"
                        style={{ backgroundColor: '#e9ecef' }} // 읽기 전용 필드임을 시각적으로 나타냄
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="currentRank" className="form-label fw-bold">현재 순위:</label>
                    <input
                        type="number"
                        id="currentRank"
                        value={formData.currentRank}
                        onChange={handleChange}
                        required
                        className="form-control"
                        // 순위는 전체 순위 계산에 따라 결정되는 것이 일반적이므로,
                        // 수동 변경을 막으려면 readOnly로 변경하는 것을 고려하세요.
                        // 여기서는 일단 수동 변경 가능하게 둠.
                    />
                </div>
                <div className="mb-4">
                    <label htmlFor="gamesBehind" className="form-label fw-bold">게임차:</label>
                    <input
                        type="number" // 타입은 'number'로 유지하여 숫자 입력에 최적화
                        id="gamesBehind"
                        value={formData.gamesBehind} // toFixed는 보여줄 때만 적용
                        onChange={handleChange} // onChange 이벤트 활성화
                        step="0.5" // 0.5 단위 입력 가능
                        required
                        className="form-control"
                        // style={{ backgroundColor: '#e9ecef' }} // 수동 입력 가능하므로 배경색 제거
                    />
                </div>
                <div className="d-flex justify-content-end gap-2">
                    <button type="submit" className="btn btn-primary rounded-pill px-4 fw-bold shadow-sm" disabled={!isFormValid}>
                        {buttonText}
                    </button>
                    <button type="button" className="btn btn-secondary rounded-pill px-4 fw-bold shadow-sm" onClick={onClose}>
                        취소
                    </button>
                </div>
            </form>
        </Modal>
    );
}

export default TeamRankingFormModal;