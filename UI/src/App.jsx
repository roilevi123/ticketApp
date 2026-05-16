import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import EventCatalog from './components/EventCatalog';
import CompanyProfile from './components/CompanyProfile';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<EventCatalog />} />
        <Route path="/company/:companyName" element={<CompanyProfile />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
