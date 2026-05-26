import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import axiosClient from "../api/axiosClient";

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
      const payload = {
        ...formData,
        age: parseInt(formData.age, 10),
      };

      const response = await axiosClient.post("/auth/register", payload);

      setSuccessMsg("Registration successful! You can now log in.");
      setTimeout(() => {
        navigate("/login");
      }, 2000);
    } catch (err) {
      const respData = err.response?.data;
      setErrorMsg(
        `Registration failed: ${respData?.error || "Please check your inputs"}`,
      );
    }
  };

  return (
    <div className="flex flex-col items-center justify-center p-8 min-h-[70vh] bg-surface-dim rounded-2xl border border-outline-variant my-8">
      <div className="bg-surface-container-highest border border-outline-variant p-8 rounded-xl w-full max-w-sm shadow-2xl">
        <h2 className="text-headline-md text-on-surface mb-6 text-center">
          Register
        </h2>

        {errorMsg && (
          <div className="bg-error-container/20 border border-error text-error text-label-md px-4 py-3 rounded mb-6">
            {errorMsg}
          </div>
        )}

        {successMsg && (
          <div className="bg-secondary/10 border border-secondary text-secondary text-label-md px-4 py-3 rounded mb-6">
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
            className="bg-surface-container-highest border border-outline-variant rounded-lg p-3 text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-secondary transition-colors"
          />
          <input
            type="email"
            placeholder="Email Address"
            required
            onChange={(e) =>
              setFormData({ ...formData, email: e.target.value })
            }
            className="bg-surface-container-highest border border-outline-variant rounded-lg p-3 text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-secondary transition-colors"
          />
          <input
            type="number"
            placeholder="Age"
            required
            min="1"
            onChange={(e) => setFormData({ ...formData, age: e.target.value })}
            className="bg-surface-container-highest border border-outline-variant rounded-lg p-3 text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-secondary transition-colors"
          />
          <input
            type="password"
            placeholder="Password"
            required
            onChange={(e) =>
              setFormData({ ...formData, password: e.target.value })
            }
            className="bg-surface-container-highest border border-outline-variant rounded-lg p-3 text-on-surface placeholder:text-on-surface-variant focus:outline-none focus:border-secondary transition-colors"
          />
          <button
            type="submit"
            className="bg-secondary text-on-secondary py-3 px-4 text-label-md font-bold rounded-lg hover:brightness-110 active:scale-95 transition-all mt-2"
          >
            Register
          </button>
        </form>

        <div className="mt-6 text-center text-body-md text-on-surface-variant">
          Already have an account?{" "}
          <Link
            to="/login"
            className="text-secondary hover:underline font-medium"
          >
            Log in here
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Register;
