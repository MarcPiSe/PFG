import { useState, useRef } from "react";
import { RiAddLine, RiFileUploadLine, RiFolderLine, RiFolderUploadLine } from "react-icons/ri";
import { Popover, PopoverContent, PopoverTrigger } from "../Popover";
import { Button } from "../Button";
import { FileItem } from "../../types";
import CreateNewFolderDialog from "../CreateNewFolderDialog";
import { useFileOperations } from '../../hooks/useFileOperations';

declare module 'react' {
	interface InputHTMLAttributes<T> extends HTMLAttributes<T> {
		webkitdirectory?: string;
		directory?: string;
	}
}

export const AddButton = () => {	
	const [isCreateFolderOpen, setIsCreateFolderOpen] = useState(false);
	const fileInputRef = useRef<HTMLInputElement>(null);
	const folderInputRef = useRef<HTMLInputElement>(null);

	const {
		currentDirectory,
		createFolder,
		uploadFile,
		uploadFolder
	} = useFileOperations();

	const handleCreateFolderLoacal = async (name: string) => {
		
		
		if (!name.trim()) {
			
			return;
		}
		
		try {
			await createFolder({ name: name, parentId: currentDirectory.id!.toString() });
			
			setIsCreateFolderOpen(false);
		} catch (error) {
			console.error('[AddButton] Error creating folder:', error);
		}
	};

	const handleFileUploadLocal = async (event: React.ChangeEvent<HTMLInputElement>) => {
		
		
		const files = event.target.files;
		if (!files || files.length === 0) {
			
			return;
		}

		const file = files[0];
		
		
		const item: FileItem = {
			id: `temp-${Date.now()}`,
			name: file.name,
			type: 'file',
			size: file.size,
			mimeType: file.type
		};

		try {
			uploadFile({ item, file, parentId: currentDirectory.id!.toString() });
			
		} catch (error) {
			console.error('[AddButton] Error uploading file:', error);
		}

		event.target.value = '';
	};

	const handleFolderUploadLocal = async (event: React.ChangeEvent<HTMLInputElement>) => {
		
		const files = event.target.files;
		if (!files || files.length === 0) {
			return;
		}
		try {
			uploadFolder({ files: Array.from(files), parentId: currentDirectory.id!.toString() });
			
		} catch (error) {
			console.error('[AddButton] Error uploading folder:', error);
		}

		event.target.value = '';
	};

	const menuContent = (
		<div className="flex flex-col p-2 gap-2">
			<div onClick={() => {
				
				setIsCreateFolderOpen(true);
			}}>
				<Button variant="ghost" className="w-full justify-start gap-2">
					<RiFolderLine className="h-4 w-4" />
					New folder
				</Button>
			</div>
			<input
				type="file"
				ref={fileInputRef}
				className="hidden"
    			accept="*"
    			multiple={false}
				onChange={handleFileUploadLocal}
			/>
			<input
				type="file"
				ref={folderInputRef}
				className="hidden"
				webkitdirectory=""
				directory=""
    			multiple={false}
				onChange={handleFolderUploadLocal}
			/>
			<Button
				variant="ghost"
				className="w-full justify-start gap-2"
				onClick={() => {
					
					fileInputRef.current?.click();
				}}
			>
				<RiFileUploadLine className="h-4 w-4" />
				Upload file
			</Button>
			<Button
				variant="ghost"
				className="w-full justify-start gap-2"
				onClick={() => {
					
					folderInputRef.current?.click();
				}}
			>
				<RiFolderUploadLine className="h-4 w-4" />
				Upload folder
			</Button>
		</div>
	);

	
	
	return (
		<>
			<Popover>
				<PopoverTrigger asChild>
					<Button 
						variant="primary" 
						className="w-full flex gap-2 items-center"
						aria-label="Add new"
					>
						<RiAddLine className="size-5" /> <span>New</span>
					</Button>
				</PopoverTrigger>
				<PopoverContent className="w-56 p-0">
					{menuContent}
				</PopoverContent>
			</Popover>
			<CreateNewFolderDialog
				isOpen={isCreateFolderOpen}
				onOpenChange={setIsCreateFolderOpen}
				onConfirm={handleCreateFolderLoacal}
			/>
		</>
	);
}; 