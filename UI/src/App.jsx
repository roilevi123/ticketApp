import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import EventCatalog from './components/EventCatalog';
import CompanyProfile from './components/CompanyProfile';
import EventDetails from './components/EventDetails';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<EventCatalog />} />
        <Route path="/company/:companyName" element={<CompanyProfile />} />
        <Route path="/event/:companyName/:eventName" element={<EventDetails />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
