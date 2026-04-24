import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import TransactionList from './pages/TransactionList'
import TemplateEditor from './pages/TemplateEditor'
import Visualizer from './pages/Visualizer'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50">
        <Routes>
          <Route path="/" element={<Navigate to="/transactions" replace />} />
          <Route path="/transactions" element={<TransactionList />} />
          <Route path="/transactions/:name/template" element={<TemplateEditor />} />
          <Route path="/transactions/:name/visualize" element={<Visualizer />} />
        </Routes>
      </div>
    </BrowserRouter>
  </StrictMode>,
)
