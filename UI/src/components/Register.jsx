import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";

function Register() {
  const [formData, setFormData] = useState({
    username: "",
    password: "",
    email: "",
    age: "",
  });
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const navigate = useNavigate();

  const handleRegister = async (e) => {
    e.preventDefault();
    setErrorMsg("");
    setSuccessMsg("");

    try {
      const response = await fetch("/api/auth/register", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer guest-temporary-token",
        },
        body: JSON.stringify({
          ...formData,
          age: parseInt(formData.age, 10),
        }),
      });
      const data = await response.json();

      if (response.ok && data.success) {
        setSuccessMsg("Registration successful! You can now log in.");
        setTimeout(() => {
          navigate("/login");
        }, 2000);
      } else {
        setErrorMsg(
          `Registration failed: ${data.message || "Please check your inputs"}`,
        );
      }
    } catch (err) {
      setErrorMsg("Registration failed: Unable to connect to server");
    }
  };

  return (
    <div className="flex flex-col items-center justify-center p-8 min-h-[50vh]">
      <div className="bg-white p-6 rounded-lg shadow-md w-full max-w-sm">
        <h2 className="text-2xl font-bold mb-4 text-center">Register</h2>

        {errorMsg && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4">
            {errorMsg}
          </div>
        )}

        {successMsg && (
          <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded relative mb-4">
            {successMsg}
          </div>
        )}

        <form onSubmit={handleRegister} className="flex flex-col gap-4">
          <input
            type="text"
            placeholder="Username"
            required
            onChange={(e) =>
              setFormData({ ...formData, username: e.target.value })
            }
            className="border border-gray-300 rounded p-2 focus:outline-none focus:border-blue-500"
          />
          <input
            type="email"
            placeholder="Email Address"
            required
            onChange={(e) =>
              setFormData({ ...formData, email: e.target.value })
            }
            className="border border-gray-300 rounded p-2 focus:outline-none focus:border-blue-500"
          />
          <input
            type="number"
            placeholder="Age"
            required
            min="1"
            onChange={(e) => setFormData({ ...formData, age: e.target.value })}
            className="border border-gray-300 rounded p-2 focus:outline-none focus:border-blue-500"
          />
          <input
            type="password"
            placeholder="Password"
            required
            onChange={(e) =>
              setFormData({ ...formData, password: e.target.value })
            }
            className="border border-gray-300 rounded p-2 focus:outline-none focus:border-blue-500"
          />
          <button
            type="submit"
            className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded"
          >
            Register
          </button>
        </form>

        <div className="mt-4 text-center text-sm">
          Already have an account?{" "}
          <Link to="/login" className="text-blue-600 hover:underline">
            Log in here
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Register;
