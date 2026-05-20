import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout'; 
import EventCatalog from './components/EventCatalog';
import CompanyProfile from './components/CompanyProfile';
import EventDetails from './components/EventDetails';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<EventCatalog />} />
          <Route path="company/:id" element={<CompanyProfile />} />
          <Route path="event/:id" element={<EventDetails />} />
          <Route path="*" element={<div className="text-center text-2xl mt-10">404 - Page Not Found</div>} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;