// src/components/TeamFormModal.js
import React, { useState, useEffect } from 'react';
import Modal from './Modal'; // Assuming Modal is in the same directory

/**
 * TeamFormModal 컴포넌트
 * 팀 추가 또는 수정에 사용되는 모달 형태의 폼입니다.
 *
 * @param {object} props - 컴포넌트 속성
 * @param {object} props.initialData - 폼의 초기 데이터 (수정 시 사용). 새 팀 등록 시에는 null 또는 빈 객체.
 * @param {function} props.onSubmit - 폼 제출 시 호출될 콜백 함수. (formData) => Promise<void>
 * @param {function} props.onClose - 모달 닫기 버튼 클릭 시 호출될 콜백 함수.
 * @param {string} props.title - 모달의 제목.
 * @param {string} props.buttonText - 제출 버튼에 표시될 텍스트.
 */
function TeamFormModal({ initialData, onSubmit, onClose, title, buttonText }) {
  // 폼 데이터를 관리하는 상태. initialData가 있으면 해당 데이터로 초기화, 없으면 기본값으로 초기화.
  const [formData, setFormData] = useState(initialData || {
    name: '',
    logoUrl: '',
  });

  // initialData가 변경될 때마다 폼 데이터를 업데이트합니다.
  useEffect(() => {
    if (initialData) {
      setFormData(initialData);
    } else {
      setFormData({ name: '', logoUrl: '' });
    }
  }, [initialData]);

  // 입력 필드 변경 핸들러
  const handleChange = (e) => {
    const { id, value } = e.target;
    setFormData(prevData => ({
      ...prevData,
      [id]: value,
    }));
  };

  // 폼 제출 핸들러
  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(formData); // 부모 컴포넌트에서 전달받은 onSubmit 함수 호출
  };

  return (
    <Modal onClose={onClose} message={null} title={title}>
      <form onSubmit={handleSubmit} className="p-4 bg-light rounded-lg shadow-sm">
        <div className="mb-3">
          <label htmlFor="name" className="form-label fw-bold">팀 이름:</label>
          <input
            type="text"
            id="name"
            value={formData.name}
            onChange={handleChange}
            required
            className="form-control"
          />
        </div>
        <div className="mb-3">
          <label htmlFor="logoUrl" className="form-label fw-bold">로고 URL:</label>
          <input
            type="url" // URL 입력에 적합한 타입
            id="logoUrl"
            value={formData.logoUrl}
            onChange={handleChange}
            required
            className="form-control"
          />
        </div>
        <div className="d-flex justify-content-end gap-2 mt-4">
          <button type="submit" className="btn btn-primary rounded-pill px-4 fw-bold shadow-sm">
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

export default TeamFormModal;