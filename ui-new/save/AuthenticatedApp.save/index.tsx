import { Navigate, Route, Routes } from "react-router-dom";
import { routes } from "../../src/config/routes";
import { ProtectedRoute } from "./ProtectedRoute";
import AppLayout from "../../src/layouts/AppLayout";

function AppRoutes() {
	const publicRoutes = Object.values(routes).filter((route) => !route.private);
	const privateRoutes = Object.values(routes).filter((route) => route.private);

	return (
		<Routes>
			{publicRoutes.map((section) => {
				return (
					<Route
						key={section.path}
						path={section.path}
						element={section.element}
					/>
				);
			})}

			{privateRoutes.map((section) => {
				return (
					<Route
						element={<AppLayout />}
					>
						<Route
							key={section.path}
							path={section.path}
							element={
								<ProtectedRoute>
									{section.element}
								</ProtectedRoute>
							}
						/>
					</Route>

				);
			})}

			{/* "404 page" */}
			<Route path="*" element={<Navigate replace to="/login" />} />
		</Routes>
	);
}

const AuthenticatedApp = () => {
	return (
		<>
			{/* <UserAuthProvider> */}
			<AppRoutes />
			{/* </UserAuthProvider> */}
		</>
	);
}

export default AuthenticatedApp