import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../src/hooks/userAuth";

interface ProtectedRouteProps {
	children: React.ReactNode;
}

export const ProtectedRoute = ({ children }: ProtectedRouteProps) => {
	

	const { isLogin } = useAuth();
	const location = useLocation();

	if (!isLogin) {
		
		return <Navigate to="/login" state={{ from: location }} replace />;
	}

	
	return <>{children}</>;
};
