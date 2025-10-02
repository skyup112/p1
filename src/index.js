// src/index.js
import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css'; // 전역 CSS 파일 (선택 사항)
import AppWrapper from './App'; // AppWrapper 컴포넌트 임포트

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <AppWrapper /> {/* AppWrapper를 렌더링하여 App 컴포넌트가 Router 내부에 있도록 함 */}
  </React.StrictMode>
);