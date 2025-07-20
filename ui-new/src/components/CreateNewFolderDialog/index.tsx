import { useState, useCallback } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../Dialog"
import { Button } from "../Button"
import { Input } from "../Input"
import { useNotificationService } from "../../services/notificationService"

interface CreateNewFolderDialogProps {
	isOpen: boolean;
	onOpenChange: (open: boolean) => void;
	onConfirm: (name: string) => Promise<void>;
}

const CreateNewFolderDialog = ({
	isOpen,
	onOpenChange,
	onConfirm
}: CreateNewFolderDialogProps) => {
	

	const [folderName, setFolderName] = useState("")
	const [isLoading, setIsLoading] = useState(false)
	const notifications = useNotificationService();

	const handleSubmit = useCallback(async (e: React.FormEvent) => {
		e.preventDefault()
		

		if (!folderName.trim()) {
			
			return;
		}

		setIsLoading(true)
		
		
		try {
			await onConfirm(folderName.trim())
			
			setFolderName("")
			onOpenChange(false)
			notifications.success('Folder created successfully');
		} catch (error) {
			console.error('[CreateNewFolderDialog] Error creating folder:', error);
			notifications.error('Error creating folder');
		} finally {
			
			setIsLoading(false)
		}
	}, [folderName, onConfirm, onOpenChange, notifications]);

	const handleCancel = useCallback(() => {
		
		onOpenChange(false);
	}, [onOpenChange]);

	const handleNameChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
		
		setFolderName(e.target.value);
	}, []);

	if (!isOpen) {
		
		return null;
	}

	
	return (
		<Dialog open={isOpen} onOpenChange={(open) => {
			
			onOpenChange(open);
		}}>
			<DialogContent className="sm:max-w-lg">
				<form onSubmit={handleSubmit}>
					<DialogHeader>
						<DialogTitle>New folder</DialogTitle>
						<DialogDescription>
							Enter the name for the new folder
						</DialogDescription>
					</DialogHeader>
					<div className="py-4">
						<Input
							value={folderName}
							onChange={handleNameChange}
							placeholder="Folder name"
							autoFocus
						/>
					</div>
					<DialogFooter className="mt-6">
						<Button
							type="button"
							variant="secondary"
							onClick={handleCancel}
							disabled={isLoading}
						>
							Cancel
						</Button>
						<Button
							type="submit"
							disabled={!folderName.trim() || isLoading}
							isLoading={isLoading}
						>
							Create
						</Button>
					</DialogFooter>
				</form>
			</DialogContent>
		</Dialog>
	)
}

export default CreateNewFolderDialog