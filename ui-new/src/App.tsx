import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { routes } from "./config/routes";
import { useAuth } from "./hooks/userAuth";
import AppLayout from "./layouts/AppLayout";
import { Toaster } from "./components/Toaster";
import { useEffect, useState } from "react";
import { useFileStore } from "./store/fileStore";
import { Card } from "./components/Card";

const App = () => {
    const [isLoading, setIsLoading] = useState(true);
    const [isSignUpMode, setIsSignUpMode] = useState(false);
    
    const { user } = useAuth();
    const { fetchInitialState } = useFileStore();
    
    useEffect(() => {
        (async () => {
            await fetchInitialState();
            setIsLoading(false);    
        })();
    }, []);

    if(isLoading) {
        return (
            <div id="45" className="container mx-auto py-6">
                <Card className="p-6 text-center">
                    <h2 className="text-2xl font-bold mb-4">Loading...</h2>
                    <p className="text-gray-600 mb-4">Please wait while data loads.</p>
                </Card>
            </div>
        );
    }

    return (
        <Router>
            {!user && (
                <div className="fixed top-4 right-4 z-50 bg-white shadow-lg rounded-lg p-4 border">
                    <div className="flex items-center space-x-3">
                        <span className={`text-sm font-medium ${!isSignUpMode ? 'text-blue-600' : 'text-gray-500'}`}>
                            Login
                        </span>
                        <label className="relative inline-flex items-center cursor-pointer">
                            <input
                                type="checkbox"
                                className="sr-only peer"
                                checked={isSignUpMode}
                                onChange={(e) => setIsSignUpMode(e.target.checked)}
                            />
                            <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                        </label>
                        <span className={`text-sm font-medium ${isSignUpMode ? 'text-blue-600' : 'text-gray-500'}`}>
                            Signup
                        </span>
                    </div>
                </div>
            )}
            
            <Routes>
                <Route
                    path={routes.login.path}
                    element={
                        user ? <Navigate to="/" replace /> : (
                            isSignUpMode ? <Navigate to="/sign-up" replace /> : routes.login.element
                        )
                    }
                />
                <Route
                    path={routes.signUp.path}
                    element={
                        user ? <Navigate to="/" replace /> : (
                            !isSignUpMode ? <Navigate to="/login" replace /> : routes.signUp.element
                        )
                    }
                />
                <Route
                    path="/signup"
                    element={<Navigate to="/sign-up" replace />}
                />
                <Route element={<AppLayout />}>
                    <Route
                        path={routes.root.path}
                        element={
                            !user ? (
                                <Navigate to="/login" replace />
                            ) : (
                                routes.root.element
                            )
                        }
                    />
                    
                </Route>
            </Routes>
            <Toaster />
        </Router>
    );
};

export default App;
