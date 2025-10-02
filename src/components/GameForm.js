import React, { useState, useEffect, useMemo } from 'react';
import { teamApi } from '../services/apiService'; // teamApi 임포트

/**
 * GameForm 컴포넌트
 * 새 경기 등록 또는 기존 경기 수정에 사용되는 재사용 가능한 폼입니다.
 *
 * @param {object} props - 컴포넌트 속성
 * @param {object} props.initialData - 폼의 초기 데이터 (수정 시 사용). 새 경기 등록 시에는 null 또는 빈 객체.
 * @param {function} props.onSubmit - 폼 제출 시 호출될 콜백 함수. (formData) => Promise<void>
 * @param {function} [props.onCancel] - 취소 버튼 클릭 시 호출될 콜백 함수.
 * @param {string} props.buttonText - 제출 버튼에 표시될 텍스트.
 * @param {string} props.title - 폼의 제목.
 */
function GameForm({ initialData, onSubmit, onCancel, buttonText, title }) {
    // 모든 팀 데이터를 저장할 상태
    const [allTeams, setAllTeams] = useState([]);
    // '롯데 자이언츠'를 제외한 상대 팀 목록 (API에서 가져온 데이터로 필터링)
    const predefinedOpponents = useMemo(() => {
        // allTeams가 로드되면 '롯데 자이언츠'를 제외한 팀들의 이름과 ID를 추출
        return allTeams.filter(team => team.name !== '롯데 자이언츠').map(team => ({ id: team.id, name: team.name })); // ID도 함께 저장
    }, [allTeams]); // allTeams가 변경될 때마다 재계산

    // 폼 데이터를 관리하는 상태. initialData가 있으면 해당 데이터로 초기화, 없으면 기본값으로 초기화.
    const [formData, setFormData] = useState(initialData || {
        gameDate: '',
        // opponent 필드는 이제 선택된 팀의 이름만 저장 (드롭다운 표시용)
        opponent: '', 
        // NEW: opponentTeam 필드에 선택된 Team 객체를 저장
        opponentTeam: null, 
        location: '',
        homeScore: 0,
        awayScore: 0,
    });

    const [loadingTeams, setLoadingTeams] = useState(true); // 팀 로딩 상태

    // 컴포넌트 마운트 시 모든 팀 데이터를 API에서 불러옵니다.
    useEffect(() => {
        const fetchTeams = async () => {
            try {
                setLoadingTeams(true);
                const response = await teamApi.getAllTeams();
                setAllTeams(response.data);
            } catch (error) {
                console.error('팀 목록을 불러오는 데 실패했습니다:', error);
            } finally {
                setLoadingTeams(false);
            }
        };
        fetchTeams();
    }, []); // 빈 배열: 컴포넌트 마운트 시 한 번만 실행

    // initialData 또는 predefinedOpponents가 변경될 때마다 폼 데이터를 업데이트합니다.
    useEffect(() => {
        if (!loadingTeams) { // 팀 목록 로딩이 완료된 후에 실행
            if (initialData) {
                const initialOpponentName = initialData.opponentTeam ? initialData.opponentTeam.name : (initialData.opponent || '');
                const initialOpponentObject = initialData.opponentTeam ? { id: initialData.opponentTeam.id, name: initialOpponentName } : null;
                setFormData({
                    ...initialData,
                    opponent: initialOpponentName,
                    opponentTeam: initialOpponentObject, // 초기 데이터에 Team 객체 설정
                    gameDate: initialData.gameDate ? initialData.gameDate.substring(0, 16) : '',
                });
            } else {
                // 새 경기 등록 시 폼 초기화 및 첫 번째 미리 정의된 팀으로 설정
                const defaultOpponent = predefinedOpponents[0];
                setFormData({
                    gameDate: '',
                    opponent: defaultOpponent ? defaultOpponent.name : '', // 첫 번째 팀 이름으로 기본값 설정
                    opponentTeam: defaultOpponent || null, // 첫 번째 Team 객체로 기본값 설정
                    location: '',
                    homeScore: 0,
                    awayScore: 0,
                });
            }
        }
    }, [initialData, loadingTeams, predefinedOpponents]); // loadingTeams와 predefinedOpponents를 의존성 배열에 추가

    // 입력 필드 변경 핸들러
    const handleChange = (e) => {
        const { id, value, type } = e.target;

        if (id === 'opponent') {
            const selectedTeam = predefinedOpponents.find(team => team.name === value);
            setFormData(prevData => ({
                ...prevData,
                opponent: value, // 드롭다운에 표시될 이름
                opponentTeam: selectedTeam || null, // 선택된 팀의 전체 객체 (ID와 이름 포함)
            }));
        } else {
            setFormData(prevData => ({
                ...prevData,
                [id]: type === 'number' ? parseInt(value) || 0 : value, // 숫자 타입은 정수로 변환
            }));
        }
    };

    // 폼 제출 핸들러
    const handleSubmit = (e) => {
        e.preventDefault();
        // onSubmit으로 전달되는 formData에는 이제 opponentTeam 객체가 포함됩니다.
        onSubmit(formData); 
    };

    if (loadingTeams) {
        return <div className="text-center py-4 text-secondary">팀 목록을 불러오는 중...</div>;
    }

    return (
        // div.container로 form을 감싸서 가로폭을 맞춥니다.
        <div className="container my-3">
            <form onSubmit={handleSubmit} className="p-4 bg-light rounded-lg shadow-sm">
                <h3 className="text-center text-dark mb-4">{title}</h3>
                <div className="mb-3">
                    <label htmlFor="gameDate" className="form-label fw-bold">경기 날짜:</label>
                    <input
                        type="datetime-local"
                        id="gameDate"
                        value={formData.gameDate}
                        onChange={handleChange}
                        required
                        className="form-control"
                    />
                </div>
                <div className="mb-3">
                    <label className="form-label fw-bold d-block">상대 팀:</label>
                    <select
                        id="opponent"
                        value={formData.opponent}
                        onChange={handleChange}
                        required
                        className="form-select"
                    >
                        <option value="">상대 팀을 선택하세요</option>
                        {/* predefinedOpponents 배열을 사용하여 옵션 렌더링 */}
                        {predefinedOpponents.map((team) => ( // team 객체 사용
                            <option key={team.id} value={team.name}>{team.name}</option>
                        ))}
                    </select>
                </div>
                <div className="mb-3">
                    <label htmlFor="location" className="form-label fw-bold">장소:</label>
                    <input
                        type="text"
                        id="location"
                        value={formData.location}
                        onChange={handleChange}
                        required
                        className="form-control"
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="homeScore" className="form-label fw-bold">홈 점수:</label>
                    <input
                        type="number"
                        id="homeScore"
                        value={formData.homeScore}
                        onChange={handleChange}
                        required
                        className="form-control"
                    />
                </div>
                <div className="mb-4">
                    <label htmlFor="awayScore" className="form-label fw-bold">원정 점수:</label>
                    <input
                        type="number"
                        id="awayScore"
                        value={formData.awayScore}
                        onChange={handleChange}
                        required
                        className="form-control"
                    />
                </div>
                <div className="d-flex justify-content-end gap-2">
                    <button type="submit" className="btn btn-primary rounded-pill px-4 fw-bold shadow-sm">
                        {buttonText}
                    </button>
                    {onCancel && (
                        <button type="button" className="btn btn-secondary rounded-pill px-4 fw-bold shadow-sm" onClick={onCancel}>
                            취소
                        </button>
                    )}
                </div>
            </form>
        </div>
    );
}

export default GameForm;