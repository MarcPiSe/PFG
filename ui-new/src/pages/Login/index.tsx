import { useReducer } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Input } from "../../components/Input";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { useAuth } from "../../hooks/userAuth";
import { useNotificationService } from '../../services/notificationService';
import { useValidation } from "../../hooks/useValidation";
import { useState } from "react";

const initialState = {
	username: "user1",
	password: "ComplexPassword123!",
	errors: {
		username: "",
		password: "",
	},
	isLoading: false,
};

type LoginAction = 
	| { type: "SET_USERNAME" | "SET_PASSWORD"; payload: string }
	| { type: "SET_ERROR"; field: keyof typeof initialState["errors"]; payload: string }
	| { type: "CLEAR_ERROR"; field: keyof typeof initialState["errors"] }
	| { type: "SET_LOADING"; payload: boolean };

const reducer = (state: typeof initialState, action: LoginAction) => {
	switch (action.type) {
		case "SET_USERNAME":
			return {
				...state,
				username: action.payload,
			};
		case "SET_PASSWORD":
			return {
				...state,
				password: action.payload,
			};
		case "SET_ERROR":
			return {
				...state,
				errors: {
					...state.errors,
					[action.field]: action.payload,
				},
			};
		case "CLEAR_ERROR":
			return {
				...state,
				errors: {
					...state.errors,
					[action.field]: "",
				},
			};
		case "SET_LOADING":
			return {
				...state,
				isLoading: action.payload,
			};
		default:
			return state;
	}
};

const ERRORS = {
	username: "Username is required",
	password: "Password must be at least 6 characters long",
};

const validateUsername = (username: string) => {
	if (!username.trim()) {
		return ERRORS.username;
	}
	return "";
};

const validatePassword = (password: string) => {
	if (password.length < 6) {
		return ERRORS.password;
	}
	return "";
};

export const Login = () => {
	

	const [state, dispatch] = useReducer(reducer, initialState);
	const navigate = useNavigate();
	const notifications = useNotificationService();
	const { login } = useAuth();
	const { errors, validateLoginForm, clearErrors } = useValidation();
	const [isLoading, setIsLoading] = useState(false);

	const { username, password } = state;

	const nameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
		const usernameValue = event.target.value;
		clearErrors();
		dispatch({ type: "SET_USERNAME", payload: usernameValue });
		dispatch({
			type: "SET_ERROR",
			field: "username",
			payload: validateUsername(usernameValue),
		});
	};

	const passChange = (event: React.ChangeEvent<HTMLInputElement>) => {
		const passwordValue = event.target.value;
		clearErrors();
		dispatch({ type: "SET_PASSWORD", payload: passwordValue });
		dispatch({
			type: "SET_ERROR",
			field: "password",
			payload: validatePassword(passwordValue),
		});
	};

	const submit = async (event: React.FormEvent<HTMLFormElement>) => {
		event.preventDefault();
		
		const isValid = validateLoginForm({ username, password });
		if (!isValid) {
			notifications.error("Please correct the errors in the form");
			return;
		}

		try {
			setIsLoading(true);
			const success = await login(username, password);
			
			if (success) {
				notifications.success("Login successful");
				navigate("/");
			}
		} catch (error: unknown) {
			console.error('[Login] Login failed:', error);
			const errorMessage = error instanceof Error 
				? error.message 
				: "Unable to log in. Please check your credentials.";
			
			notifications.error(errorMessage);
		} finally {
			setIsLoading(false);
		}
	};

	const disabledButton = !!(
		errors.username ||
		errors.password ||
		!username ||
		!password ||
		isLoading
	);

	return (
		<main className="px-4">
			<div className="max-w-lg mx-auto flex flex-col justify-center h-screen gap-10">
				<Card>
					<div className="flex items-center justify-between mb-4">
						<h2 className="text-2xl font-bold capitalize">
							Sign In
						</h2>
						<Button variant="ghost" asChild className="w-1/6 capitalize">
							<Link to="/sign-up">Sign Up</Link>
						</Button>
					</div>
					<form className="grid grid-cols-1 gap-4" onSubmit={submit}>
						<div>
							<label>
								<span className="capitalize">Email</span>
								<Input
									value={username}
									onChange={nameChange}
									onFocus={() =>
										dispatch({ type: "CLEAR_ERROR", field: "username" })
									}
									hasError={!!errors.username}
									placeholder="Enter your username"
								/>
								{errors.username && <p className="text-red-500 text-sm mt-1">{errors.username}</p>}
							</label>
						</div>
						<div>
							<label>
								<span className="capitalize">Password</span>
								<Input
									value={password}
									onChange={passChange}
									onFocus={() =>
										dispatch({ type: "CLEAR_ERROR", field: "password" })
									}
									hasError={!!errors.password}
									placeholder="Enter your password here"
									type="password"
								/>
								{errors.password && <p className="text-red-500 text-sm mt-1">{errors.password}</p>}
							</label>
						</div>

						<Button
							type="submit"
							disabled={disabledButton}
							className="w-full"
						>
							{isLoading ? "Signing in..." : "Sign In"}
						</Button>
					</form>
				</Card>
			</div>
		</main>
	);
};

export default Login;