import React, { useState, useEffect } from 'react';
import Modal from './Modal'; // Generic modal component

function MemberEditModal({ member, onClose, onSubmit }) {
    // Initialize editedData with a copy of the member prop
    const [editedData, setEditedData] = useState({ ...member });
    const [modalMessage, setModalMessage] = useState(''); // Internal modal message state

    useEffect(() => {
        // Reset editedData if the member prop changes (e.g., a different member is selected)
        setEditedData({ ...member });
    }, [member]);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setEditedData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleDateChange = (e) => {
        const value = e.target.value;
        setEditedData(prev => ({
            ...prev,
            // Convert to ISO string for backend, or null if empty
            bannedUntil: value ? new Date(value).toISOString() : null
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setModalMessage(''); // Clear previous messages

        // Client-side validation for role
        const validRoles = ['USER', 'ADMIN'];
        if (!validRoles.includes(editedData.role.toUpperCase())) {
            setModalMessage('유효하지 않은 역할입니다. (USER 또는 ADMIN)');
            return;
        }

        // Validate bannedUntil date if banned is true and date is provided
        if (editedData.banned && editedData.bannedUntil) {
            const bannedUntilDate = new Date(editedData.bannedUntil);
            if (isNaN(bannedUntilDate.getTime())) {
                setModalMessage('유효하지 않은 정지 기한 날짜 형식입니다.');
                return;
            }
        }

        // IMPORTANT: Do NOT send password, username, or id in the body for this update
        // Password changes have a separate endpoint. Username is immutable. ID is in path.
        const dataToSend = { ...editedData };
        delete dataToSend.password;
        delete dataToSend.username;
        delete dataToSend.id;

        onSubmit(dataToSend); // Call the onSubmit prop from the parent (AdminMembers)
    };

    const handleModalClose = () => {
        setModalMessage(''); // Clear any internal messages
        onClose(); // Call the onClose prop from the parent
    };

    // Helper to format LocalDateTime (ISO string) for datetime-local input
    const formatLocalDateTimeForInput = (isoString) => {
        if (!isoString) return '';
        try {
            const date = new Date(isoString);
            const year = date.getFullYear();
            const month = (date.getMonth() + 1).toString().padStart(2, '0');
            const day = date.getDate().toString().padStart(2, '0');
            const hours = date.getHours().toString().padStart(2, '0');
            const minutes = date.getMinutes().toString().padStart(2, '0');
            return `${year}-${month}-${day}T${hours}:${minutes}`;
        } catch (e) {
            console.error("날짜 포맷팅 오류:", e);
            return '';
        }
    };

    return (
        <div className="modal d-block" tabIndex="-1" role="dialog" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
            <div className="modal-dialog modal-dialog-centered" role="document">
                <div className="modal-content">
                    <div className="modal-header bg-primary text-white">
                        <h5 className="modal-title">회원 정보 수정</h5>
                        <button type="button" className="btn-close btn-close-white" aria-label="Close" onClick={handleModalClose}></button>
                    </div>
                    <form onSubmit={handleSubmit}>
                        <div className="modal-body">
                            <div className="mb-3">
                                <label htmlFor="username" className="form-label fw-bold">아이디:</label>
                                {/* Username is disabled as it's typically not editable */}
                                <input type="text" id="username" name="username" className="form-control" value={editedData.username || ''} disabled />
                            </div>
                            <div className="mb-3">
                                <label htmlFor="name" className="form-label fw-bold">이름:</label>
                                <input type="text" id="name" name="name" className="form-control" value={editedData.name || ''} onChange={handleChange} required />
                            </div>
                            <div className="mb-3">
                                <label htmlFor="nickname" className="form-label fw-bold">닉네임:</label>
                                <input type="text" id="nickname" name="nickname" className="form-control" value={editedData.nickname || ''} onChange={handleChange} required />
                            </div>
                            <div className="mb-3">
                                <label htmlFor="email" className="form-label fw-bold">이메일:</label>
                                <input type="email" id="email" name="email" className="form-control" value={editedData.email || ''} onChange={handleChange} required />
                            </div>
                            <div className="mb-3">
                                <label htmlFor="phoneNumber" className="form-label fw-bold">전화번호:</label>
                                <input type="tel" id="phoneNumber" name="phoneNumber" className="form-control" value={editedData.phoneNumber || ''} onChange={handleChange} required />
                            </div>
                            <div className="mb-3">
                                <label htmlFor="role" className="form-label fw-bold">역할:</label>
                                <select id="role" name="role" className="form-select" value={editedData.role || ''} onChange={handleChange} required>
                                    <option value="USER">USER</option>
                                    <option value="ADMIN">ADMIN</option>
                                </select>
                            </div>
                            <div className="mb-3 form-check">
                                <input type="checkbox" id="banned" name="banned" className="form-check-input" checked={editedData.banned || false} onChange={handleChange} />
                                <label htmlFor="banned" className="form-check-label fw-bold">정지 여부</label>
                            </div>
                            {editedData.banned && ( // Only show bannedUntil if banned is checked
                                <div className="mb-3">
                                    <label htmlFor="bannedUntil" className="form-label fw-bold">정지 기한:</label>
                                    <input
                                        type="datetime-local"
                                        id="bannedUntil"
                                        name="bannedUntil"
                                        className="form-control"
                                        value={formatLocalDateTimeForInput(editedData.bannedUntil)}
                                        onChange={handleDateChange}
                                    />
                                    <small className="form-text text-muted">영구 정지는 날짜를 비워두세요.</small>
                                </div>
                            )}
                        </div>
                        <div className="modal-footer d-flex justify-content-center">
                            <button type="submit" className="btn btn-primary rounded-pill px-4 fw-bold">저장</button>
                            <button type="button" onClick={handleModalClose} className="btn btn-secondary rounded-pill px-4 fw-bold">취소</button>
                        </div>
                    </form>
                </div>
            </div>
            {/* Internal modal for validation messages within the edit modal */}
            <Modal message={modalMessage} onClose={() => setModalMessage('')} />
        </div>
    );
}

export default MemberEditModal;