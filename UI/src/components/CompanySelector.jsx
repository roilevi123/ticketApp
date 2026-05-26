import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../contexts/AuthContext';

export default function CompanySelector() {
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(true);
  const { login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchCompanies = async () => {
      try {
        const response = await axiosClient.get('/auth/my-companies');
        setCompanies(response.data);
      } catch (err) {
        console.error("Failed to load companies", err);
      } finally {
        setLoading(false);
      }
    };
    fetchCompanies();
  }, []);

  const handleSelectCompany = async (companyName) => {
    try {
      const response = await axiosClient.post('/auth/switch-company', { companyName });
      const newToken = response.data.token;
      
      // כאן אנחנו מבצעים את "שדרוג הטוקן" שנכנס ל-AuthContext
      // הפונקציה login ב-Context צריכה לדעת לעדכן את הטוקן והרול
      login(newToken); 
      navigate('/producer-dashboard');
    } catch (err) {
      alert("Failed to switch context");
    }
  };

  if (loading) return <div>Loading...</div>;

  if (companies.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] p-8">
        <h2 className="text-2xl font-bold mb-4">You are not a part of any company</h2>
        <button 
          onClick={() => navigate('/create-company')} // תוודא שיש לך מסך כזה
          className="bg-blue-600 text-white px-8 py-4 text-lg font-bold rounded-xl shadow-lg hover:bg-blue-700"
        >
          Create New Company
        </button>
      </div>
    );
  }

  return (
    <div className="p-8 max-w-2xl mx-auto">
      <h2 className="text-2xl font-bold mb-6">Select a Company to Manage</h2>
      <div className="grid gap-4">
        {companies.map(company => (
          <button 
            key={company}
            onClick={() => handleSelectCompany(company)}
            className="p-6 bg-white border border-gray-200 rounded-xl shadow-sm hover:shadow-md transition-shadow text-left font-semibold text-xl"
          >
            {company}
          </button>
        ))}
      </div>
      <button 
        onClick={() => navigate('/create-company')}
        className="mt-8 text-blue-600 font-medium hover:underline"
      >
        + Create another company
      </button>
    </div>
  );
}