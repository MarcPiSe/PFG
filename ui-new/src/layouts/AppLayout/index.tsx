import { useReducer, useCallback, useMemo, useRef, useState } from 'react';
import { DndContext, DragEndEvent } from '@dnd-kit/core';
import { Outlet } from 'react-router-dom';
import { useFileSelection } from '../../hooks/useFileSelection';
import { useFileOperations } from '../../hooks/useFileOperations';
import { useNotificationService } from '../../services/notificationService';
import { FileItem, FolderStructure } from '../../types';
import Header from './Header';
import SubHeader from './SubHeader';
import FileDirectorySidebar from '../../components/FileDirectorySidebar';
import { AddButton } from '../../components/AddButton';
import React from 'react';
import { AdminDashboard } from '../../pages/AdminDashboard';


/*type State = {
	files: FolderStructure;
	currentDirectory: FileItem | undefined;
	selectedItems: FileItem[];
};

type Action = 
	| { type: 'SET_FILES'; payload: FolderStructure }
	| { type: 'SET_CURRENT_DIRECTORY'; payload: FileItem }
	| { type: 'SET_SELECTED_ITEMS'; payload: FileItem[] }
	| { type: 'CLEAR_SELECTION' };

const initialState: State = {
	files: {
		root: { id: 'root', name: 'Root', type: 'folder', files: [], subfolders: [] },
		shared: { id: 'shared', name: 'Shared', type: 'folder', files: [], subfolders: [] },
		trash: { id: 'trash', name: 'Trash', type: 'folder', files: [], subfolders: [] }
	},
	currentDirectory: undefined,
	selectedItems: []
};

function fileReducer(state: State, action: Action): State {
	switch (action.type) {
		case 'SET_FILES':
			return { ...state, files: action.payload };
		case 'SET_CURRENT_DIRECTORY':
			return { ...state, currentDirectory: action.payload };
		case 'SET_SELECTED_ITEMS':
			return { ...state, selectedItems: action.payload };
		case 'CLEAR_SELECTION':
			return { ...state, selectedItems: [] };
		default:
			return state;
	}
}


const DragDropWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
	const [state, dispatch] = useReducer(fileReducer, initialState);
	const fileOperations = useFileOperations();
	const notifications = useNotificationService();

	const handleDragEnd = useCallback(async (event: DragEndEvent) => {
		const { active, over } = event;

		if (!over) return;

		const draggedFile = (active.data.current as FileItem);
		const overData = over.data.current || {};
		const targetFolder = overData.folderId || over.id.toString().replace('droppable-folder', '');

		if (draggedFile.id?.toString() === targetFolder) return;

		try {
			await fileOperations.moveItem({
				items: state.selectedItems?.length > 0 ? state.selectedItems : [draggedFile],
				toFolderId: targetFolder,
				fromFolderId: fileOperations.currentDirectory.id || 'root'
			});
			dispatch({ type: 'CLEAR_SELECTION' });
			notifications.success('Elemento movido correctamente');
		} catch (error) {
			console.error('Error al mover el archivo:', error);
			notifications.error('No se pudo mover el elemento');
		}
	}, [state.selectedItems, fileOperations, notifications]);

	return (
		<DndContext onDragEnd={handleDragEnd}>
			{children}
		</DndContext>
	);
};*/

const AppLayout: React.FC = () => {
	//const [state, dispatch] = useReducer(fileReducer, initialState);
	const mainContentRef = useRef<HTMLDivElement>(null);
	const [adminOpen, setAdminOpen] = useState(false);

	const { 
		handleOutsideClick, 
	} = useFileSelection({containerRef: mainContentRef});

	
	/*const outletContext = useMemo(() => ({
		files: state.files,
		currentDirectory: state.currentDirectory,
		selectedItems: state.selectedItems,
		setFiles: (files: FolderStructure) => dispatch({ type: 'SET_FILES', payload: files }),
		setCurrentDirectory: (dir: FileItem) => dispatch({ type: 'SET_CURRENT_DIRECTORY', payload: dir })
	}), [state.files, state.currentDirectory, state.selectedItems]);
	*/

	if(adminOpen) {
		return (
			<div className="flex h-screen bg-gray-100 relative">
				<button
					className="absolute top-6 right-8 z-50 px-4 py-2 bg-indigo-600 text-white rounded-lg shadow hover:bg-indigo-700 transition-colors font-semibold"
					onClick={() => setAdminOpen(false)}
				>
					Volver
				</button>
				<main className="flex-1 overflow-auto">
					<AdminDashboard  />
				</main>
			</div>
		);
	}

	return (
		//<DragDropWrapper>
			<div className="flex h-screen bg-gray-100">
				<main className="flex-1 overflow-auto">
					<div className="flex min-h-screen flex-row bg-gray-100 text-gray-800"
						onClick={handleOutsideClick}
					>
						<aside className="sidebar w-52 -translate-x-full transform bg-gray-100 p-2 transition-transform duration-150 ease-in md:translate-x-0">
							<div className="my-3 w-full text-center">
								<span className="font-mono text-md font-bold tracking-widest">
									<span className="text-indigo-600">File</span> Manager
								</span>
							</div>
							<div className="pl-2">
								<div className="w-full">
									<AddButton />
								</div>
								<div className="mt-4 max-h-[calc(100vh-120px)] overflow-y-auto scrollbar-hide transition-all duration-300">
									<FileDirectorySidebar />
								</div>
							</div>
						</aside>
						<main 
							ref={mainContentRef}
							className="main -ml-48 flex flex-grow flex-col p-4 transition-all duration-150 ease-in md:ml-0"
						>
							<div className="sticky top-0 z-50 px-8 md:px-0">
								<Header setAdminOpen={setAdminOpen} />
								<SubHeader />
							</div>
							<div className="rounded-xl h-full bg-white">
								<Outlet />
								{/* <Outlet context={outletContext} /> */}
							</div>
						</main>
					</div>
				</main>
			</div>
		//</DragDropWrapper>
	);
};

export default AppLayout;


