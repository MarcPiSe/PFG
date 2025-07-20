import Login from "../pages/Login";
import SignUp from "../pages/SignUp";
import FileManager from "../pages/FileManager";

export interface RouteInterface {
	id: number;
	path: string;
	element: JSX.Element;
	private: boolean;
	name: string;
}

export const routes: Record<string, RouteInterface> = {
	login: {
		id: 0,
		path: "/login",
		element: <Login />,
		private: false,
		name: "login",
	},
	signUp: {
		id: 1,
		path: "/sign-up",
		element: <SignUp />,
		private: false,
		name: "login",
	},
	root: {
		id: 2,
		path: "/",
		element: <FileManager />,
		private: true,
		name: "root",
	}

};