//import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.tsx'
import './index.css'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './hooks/userAuth';
import { ToastProvider } from './components/Toast';
//import React from 'react';
//import whyDidYouRender from '@welldone-software/why-did-you-render';



const queryClient = new QueryClient();


/*if (process.env.NODE_ENV === 'development') {
  whyDidYouRender(React, {
    trackAllPureComponents: true,
    logOnDifferentValues: true,
    trackHooks: true
  });
}*/


createRoot(document.getElementById('root')!).render(
//   <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
//  </StrictMode>,
)
