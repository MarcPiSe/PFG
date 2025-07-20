import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '../../components/Dialog';
import { Button } from '../../components/Button';
import { AdminUser } from '../../types';

interface DeleteUserDialogProps {
  user: AdminUser;
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export const DeleteUserDialog = ({ isOpen, onClose, onConfirm }: DeleteUserDialogProps) => {
  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Delete User</DialogTitle>
          <DialogDescription>
            Are you sure you want to delete this user? This action cannot be undone.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="destructive" onClick={onConfirm}>
            Delete
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}; 