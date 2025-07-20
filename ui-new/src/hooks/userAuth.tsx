import { useState, useCallback, createContext, useContext, ReactNode, useEffect } from "react";
import { authServiceInstance as authService, userServiceInstance as userService } from "../lib/services";
import { User } from "../types";
import { useNotificationService } from '../services/notificationService';
import { AxiosError } from "axios";
import { websocketService } from "../lib/websocket";

type AuthContextType = {
  isLogin: boolean;
  user: User | null;
  login: (username: string, password: string) => Promise<boolean>;
  logoutEndpoint: () => Promise<void>;
  register: (userData: {
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  }) => Promise<boolean>;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [isLogin, setIsLogin] = useState<boolean>(
    !!localStorage.getItem("accessToken")
  );
  const [user, setUser] = useState<User | null>(null);
  const notifications = useNotificationService();

    useEffect(() => {
    const loadUser = async () => {
      if (isLogin && !user) {          
        try {
          const userData = await userService.getCurrentUser();
          setUser(userData);
        } catch {
          setIsLogin(false);
          localStorage.removeItem("accessToken");
          localStorage.removeItem("refreshToken");
          localStorage.removeItem("username");
        }
      }
    };

    loadUser();
  }, [isLogin, user]);

  const login = useCallback(async (username: string, password: string): Promise<boolean> => {
    try {
      if(!isLogin) {
        const response = await authService.login(username, password);
        
        localStorage.setItem("accessToken", response.accessToken);
        localStorage.setItem("refreshToken", response.refreshToken);
        
        const userData = await userService.getCurrentUser();

        localStorage.setItem("username", userData.username);

        setUser(userData);
        setIsLogin(true);
        websocketService.connect();
      
      }
      return true;
    } catch (error) {
      if(error instanceof AxiosError && error.response?.status === 401) {
        notifications.error("Invalid credentials");
      } else {
        notifications.error("Login failed");
      }
      
      return false;
    }
  }, [notifications, isLogin]);

  const register = useCallback(async (userData: {
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  }): Promise<boolean> => {
    try {
      if(!isLogin) {
        const response = await authService.register(userData);
        
        localStorage.setItem("accessToken", response.accessToken);
        localStorage.setItem("refreshToken", response.refreshToken);
        
        const currentUser = await userService.getCurrentUser();

        localStorage.setItem("username", currentUser.username);

        setUser(currentUser);
        setIsLogin(true);
        websocketService.connect();
      
      }
      return true;
    } catch (error) {
      if(error instanceof AxiosError && error.response?.status === 401) {
        notifications.error("Invalid credentials");
      } else {
        notifications.error("Registration failed");
      }
      
      return false;
    }
  }, [notifications, isLogin]);

  const logoutEndpoint = useCallback(async () => {
    try {
      await authService.logout();
    } catch {
      // no linter
    } finally {
            localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("userId");
      localStorage.removeItem("username");
      
      setUser(null);
      setIsLogin(false);
      websocketService.disconnect();
      window.location.href = "/login";
    }
  }, []);

  return (
    <AuthContext.Provider value={{ isLogin, user, login, logoutEndpoint, register }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

export default useAuth; 