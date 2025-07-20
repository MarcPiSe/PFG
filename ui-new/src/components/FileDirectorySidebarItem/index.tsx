import { useMemo, useRef } from 'react';
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "../Accordion";
import { FileItem } from "../../types";
import { RiFileTextLine, RiFolder2Fill, RiArrowDownCircleFill } from "@remixicon/react";
import { useDroppable } from "@dnd-kit/core";
import { cx } from "../../lib/utils";
import { useFileOperations } from "../../hooks/useFileOperations";
import { useNotificationService } from '../../services/notificationService';
import { useFileContextStore } from "../../store/fileContextStore";
import { fileServiceInstance as fileService } from "../../lib/services";
import { useIsCurrentDirectory, useIsExpandedFolder } from '../../store/fileStore';

interface SidebarItemProps {
	file: FileItem;
	section: 'root' | 'shared' | 'trash';
	className?: string;
}

const SidebarItemAccordion = ({ file, section, className }: SidebarItemProps) => {
	const isFolderRef = useRef(file.type === 'folder');
	const notifications = useNotificationService();
	const fileContext = useFileContextStore();
	const { 
		toggleFolderExpansion,
		setCurrentDirectory
	} = useFileOperations();
	const isCurrentDirectory = useIsCurrentDirectory(file);
	const isFolderExpanded = useIsExpandedFolder(file);

	const { setNodeRef: setDroppableRef, isOver } = useDroppable({
		id: `sidebar-droppable-${file.id}`,
		disabled: file.type !== 'folder',
		data: { 
			isFolder: true,
			folderId: file.id
		}
	});

	const handleNavigate = async (e: React.MouseEvent) => {
		e.stopPropagation();
		
		if (file.type === 'folder') {
			try {
				let folderWithContents;
				if(section === 'trash') {
					folderWithContents = await fileService.getFolderById(file.id?.toString() || '', true);
				} else {
					folderWithContents = await fileService.getFolderById(file.id?.toString() || '', false);
				}
				setCurrentDirectory(folderWithContents);
				fileContext.setSection(section);
			} catch {
				notifications.error("Failed to load folder");
			}
		}
	};

	const handleToggle = () => {
		toggleFolderExpansion(file.id?.toString() || '', !isFolderExpanded);
	};

	const highlightClass = isOver && file.type === 'folder' 
		? "bg-blue-100 ring-2 ring-blue-500 shadow-md scale-105" 
		: "";

	return (
		<div 
			className={cx(
				className, 
				highlightClass, 
				"transition-all duration-200 relative rounded", 
				file.type === 'folder' ? "hover:bg-gray-100" : "",
			)}
			ref={isFolderRef.current ? setDroppableRef : undefined}
		>
			{isOver && file.type === 'folder' && (
				<div className="absolute right-2 top-1/2 transform -translate-y-1/2 text-blue-500 animate-pulse">
					<RiArrowDownCircleFill size={16} />
				</div>
			)}
			<Accordion 
				type="multiple" 
				value={isFolderExpanded ? [file.name] : []}
			>
				<AccordionItem value={file.name} className={cx("overflow-hidden", isOver && file.type === 'folder' ? "z-10" : "")}>
					<AccordionTrigger 
						disabled={file.type !== 'folder'} 
						onArrowClick={handleToggle}
						className={cx(
							"py-1 px-2 rounded",
							file.type === 'folder' ? "hover:font-medium" : "",
							isOver && file.type === 'folder' ? "font-medium" : "",
							isCurrentDirectory && "text-indigo-700 font-medium",
							isCurrentDirectory && "bg-indigo-100 border-l-4 border-indigo-500"
						)}
					>
						<span className="flex items-center gap-2 w-full">
							{file.type === 'folder' ? 
								<RiFolder2Fill 
									size={16} 
									className={cx(
										"min-w-fit",
										isOver ? "text-blue-500" : isCurrentDirectory ? "text-indigo-600" : "text-gray-800"
									)} 
								/> : 
								<RiFileTextLine 
									size={16} 
									className="min-w-fit text-gray-800" 
								/>
							}
							<span 
								className={cx(
									"text-sm cursor-pointer flex-1 min-w-0", 
									isOver && file.type === 'folder' ? "text-blue-600" : isCurrentDirectory ? "text-indigo-600" : "text-gray-800"
								)}
								onClick={handleNavigate}
							>
								<span className="truncate block w-full">{file.name}</span>
							</span>
						</span>
					</AccordionTrigger>
					{isFolderExpanded && file.subfolders && file.subfolders.length > 0 && (
						<AccordionContent className="transition-all duration-300 scrollbar-hide">
							{file.subfolders.map(folder => (
								<FileDirectorySidebarItem 
									key={folder.id}
									file={folder} 
									section={section}
									className={className}
								/>
							))}
						</AccordionContent>
					)}
				</AccordionItem>
			</Accordion>
		</div>
	);
};

const FileDirectorySidebarItem = ({ file, section, className }: SidebarItemProps) => {
	const sidebarItem = useMemo(() => {
		return (
			<SidebarItemAccordion 
				file={file} 
				section={section} 
				className={className} 
			/>
		);
	}, [file, section, className]);

	return sidebarItem;
}

export default FileDirectorySidebarItem;


