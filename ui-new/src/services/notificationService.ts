import { useToast } from '../hooks/useToast';

interface NotificationOptions {
  duration?: number;
}

export const useNotificationService = () => {
  const { toast } = useToast();

  return {
    success: (message: string, count?: number, options?: NotificationOptions) => {
      toast({
        title: "Success",
        description: count !== undefined ? `${count} ${message}` : message,
        variant: "success",
        duration: options?.duration || 3000
      });
    },

    error: (message: string, count?: number, options?: NotificationOptions) => {
      toast({
        title: "Error",
        description: count !== undefined ? `${count} ${message}` : message,
        variant: "error",
        duration: options?.duration || 3000
      });
    },

    info: (message: string, options?: NotificationOptions) => {
      toast({
        title: "Info",
        description: message,
        variant: "info",
        duration: options?.duration || 3000
      });
    },

  };
}; 