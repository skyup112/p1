import React from 'react';
// import './Modal.css'; // 기존 Modal.css 임포트 유지 또는 제거 (Bootstrap 사용 시 불필요)
// Removed any other unnecessary imports like '../api' or './CommentSection.css'

/**
 * 범용 모달 컴포넌트
 * @param {object} props - 컴포넌트 속성
 * @param {string} [props.message] - 모달 본문에 표시할 메시지
 * @param {string} [props.title] - 모달 헤더에 표시될 제목 (추가됨)
 * @param {function} props.onClose - '닫기' 또는 '취소' 버튼 클릭 시 호출될 함수
 * @param {function} [props.onConfirm] - '확인' 버튼 클릭 시 호출될 함수 (showConfirmButton이 true일 때만 유효)
 * @param {boolean} [props.showConfirmButton=false] - '확인' 버튼을 표시할지 여부
 * @param {React.ReactNode} [props.children] - 모달 본문에 렌더링될 자식 컴포넌트 (추가됨)
 */
function Modal({ message, title, onClose, onConfirm, showConfirmButton = false, children }) {
    // 메시지도 없고 자식 컴포넌트도 없으면 모달을 렌더링하지 않습니다.
    if (!message && !children) return null;

    return (
        // Bootstrap 모달 클래스 적용
        <div className="modal d-block" tabIndex="-1" role="dialog" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
            <div className="modal-dialog modal-dialog-centered" role="document">
                <div className="modal-content">
                    <div className="modal-header">
                        {/* title prop이 있으면 해당 제목을 사용, 없으면 기본 '알림' 사용 */}
                        <h5 className="modal-title">{title || '알림'}</h5>
                        <button type="button" className="btn-close" aria-label="Close" onClick={onClose}></button>
                    </div>
                    <div className="modal-body">
                        {message && <p>{message}</p>} {/* message prop이 있으면 메시지 렌더링 */}
                        {children} {/* children prop이 있으면 자식 컴포넌트 렌더링 */}
                    </div>
                    <div className="modal-footer d-flex justify-content-center">
                        {showConfirmButton && (
                            <button onClick={onConfirm} className="btn btn-primary rounded-pill px-4 fw-bold">확인</button>
                        )}
                        <button onClick={onClose} className="btn btn-secondary rounded-pill px-4 fw-bold">
                            {showConfirmButton ? '취소' : '닫기'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Modal;
