import { AccordionTrigger } from "@radix-ui/react-accordion"
import { Accordion, AccordionContent, AccordionItem } from "../Accordion"
import FileDirectorySidebarItem from "../FileDirectorySidebarItem"
import { RiDeleteBin2Line, RiFolderSharedLine, RiHome2Line, RiLoaderLine } from "@remixicon/react"
import { FileItem } from "../../types"
import { useState, useCallback, useMemo } from "react"
import { useFileOperations } from "../../hooks/useFileOperations"
import { useFileContextStore } from "../../store/fileContextStore"
import { useNotificationService } from '../../services/notificationService'
import { fileServiceInstance as fileService, sharingServiceInstance as sharingService, trashServiceInstance as trashService } from "../../lib/services"
import { useFileStore } from "../../store/fileStore"
import { cx } from "../../lib/utils"

const FileDirectorySidebar = () => {
	

	// Enable debugging for all stores
	//useFileSelectionDebug();
	//useFileStoreDebug();
	//useFileContextStoreDebug();

	const [isLoading, setIsLoading] = useState<{ [key: string]: boolean }>({
		root: false,
		shared: false,
		trash: false
	});

	const {
		folderStructure,
		folderStructureIsLoading,
		setCurrentDirectory
	} = useFileOperations();

	const fileContext = useFileContextStore();
	const notifications = useNotificationService();
	const currentDirectoryId = useFileStore((state) => state.currentDirectory?.id)
	const { setExpandedFolders } = useFileStore();
	
	const handleNavigateToSection = useCallback(async (section: 'root' | 'shared' | 'trash', e: React.MouseEvent) => {
		e.preventDefault();
		e.stopPropagation();

		setIsLoading(prev => ({ ...prev, [section]: true }));

		try {
			let folder: FileItem;
			
			switch (section) {
				case 'root':
					folder = await fileService.getRootFolder();
					break;
				case 'shared':
					folder = await sharingService.getSharedRootFolder();
					folder.id = "shared";
					break;
				case 'trash':
					folder = await trashService.getTrashRootFolder();
					folder.id = "trash";
					break;
			}

			await setCurrentDirectory(folder);

			if(fileContext.getSection() !== section) {
				await setExpandedFolders([folder.id]);
			}
			
			fileContext.setSection(section);
		} catch {
			notifications.error('Loading error');
		} finally {
			setIsLoading(prev => ({ ...prev, [section]: false }));
		}
	}, [fileContext, setCurrentDirectory, notifications, setExpandedFolders]);

	const handleRootClick = useCallback((e: React.MouseEvent) => {
		handleNavigateToSection('root', e);
	}, [handleNavigateToSection]);

	const sharedClick = useCallback((e: React.MouseEvent) => {
		handleNavigateToSection('shared', e);
	}, [handleNavigateToSection]);

	const handleTrashClick = useCallback((e: React.MouseEvent) => {
		handleNavigateToSection('trash', e);
	}, [handleNavigateToSection]);

	const rootFolders = useMemo(() => {
		if (!folderStructure?.root) return null;
		return folderStructure.root.subfolders.map((folder: FileItem) => (
			<FileDirectorySidebarItem
				key={folder.id}
				file={folder}
				section="root"
			/>
		));
	}, [folderStructure?.root]);

	const sharedFolders = useMemo(() => {
		if (!folderStructure?.shared) return null;
		return folderStructure.shared.subfolders.map((folder: FileItem) => (
			<FileDirectorySidebarItem
				key={folder.id}
				file={folder}
				section="shared"
			/>
		));
	}, [folderStructure?.shared]);

	const trashFolders = useMemo(() => {
		if (!folderStructure?.trash) return null;
		if (!folderStructure?.trash?.subfolders) return null;
		return folderStructure.trash.subfolders.map((folder: FileItem) => (
			<FileDirectorySidebarItem
				key={folder.id}
				file={folder}
				section="trash"
			/>
		));
	}, [folderStructure?.trash]);

	if (folderStructureIsLoading) {
		return (
			<div className="flex justify-center items-center h-full">
				<RiLoaderLine className="animate-spin text-2xl" />
			</div>
		);
	}

	if (!folderStructure) {
		return null;
	}

	return (
		<div className="flex flex-col gap-1 relative">
			<Accordion 
				type="single" 
				value={fileContext.getSection()}
				onValueChange={fileContext.setSection}
				collapsible
			>
				<AccordionItem value="root" className="overflow-hidden">
				<AccordionTrigger>
					<div 
						className={cx("flex items-center gap-1 text-gray-800",
							currentDirectoryId === folderStructure?.root?.id && "text-indigo-700 font-medium",
							currentDirectoryId === folderStructure?.root?.id && "bg-indigo-100 border-l-4 border-indigo-500",
							fileContext.getSection() === 'root' && "text-indigo-700 font-medium bg-indigo-50"
						)}
						onClick={handleRootClick}
					>
						{isLoading.root || folderStructureIsLoading ? 
							<RiLoaderLine className="animate-spin" /> : 
							<RiHome2Line />
						} 
						<div>Root</div>
					</div>
				</AccordionTrigger>
				<AccordionContent className="transition-all duration-300 scrollbar-hide">
					{rootFolders}
				</AccordionContent>
			</AccordionItem>
			<AccordionItem value="shared" className="overflow-hidden">
				<AccordionTrigger>
					<div 
						className={cx("flex items-center gap-1 text-gray-800",
							currentDirectoryId === folderStructure?.shared?.id && "text-indigo-700 font-medium",
							currentDirectoryId === folderStructure?.shared?.id && "bg-indigo-100 border-l-4 border-indigo-500",
							fileContext.getSection() === 'shared' && "text-indigo-700 font-medium bg-indigo-100"
						)}
						onClick={sharedClick}
					>
						{isLoading.shared || folderStructureIsLoading ? 
							<RiLoaderLine className="animate-spin" /> : 
							<RiFolderSharedLine />
						}
						<div>Shared</div>
					</div>
				</AccordionTrigger>
				<AccordionContent className="transition-all duration-300 scrollbar-hide">
					{sharedFolders}
				</AccordionContent>
			</AccordionItem>
			<AccordionItem value="trash" className="overflow-hidden">
				<AccordionTrigger>
					<div 
						className={cx("flex items-center gap-1 text-gray-800",
							currentDirectoryId === folderStructure?.trash?.id && "text-indigo-700 font-medium",
							currentDirectoryId === folderStructure?.trash?.id && "bg-indigo-100 border-l-4 border-indigo-500",
							fileContext.getSection() === 'trash' && "text-indigo-700 font-medium bg-indigo-50"
						)}
						onClick={handleTrashClick}
					>
						{isLoading.trash || folderStructureIsLoading ? 
							<RiLoaderLine className="animate-spin" /> : 
							<RiDeleteBin2Line />
						}
						<div>Trash</div>
					</div>
				</AccordionTrigger>
				<AccordionContent className="transition-all duration-300 scrollbar-hide">
					{trashFolders}
				</AccordionContent>
			</AccordionItem>
			</Accordion>
		</div>
	)
}

export default FileDirectorySidebar