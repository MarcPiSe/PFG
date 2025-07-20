import { Button } from "../Button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../Dialog";
import { Input } from "../Input";
import { FileItem } from "../../types";
import { useState, useEffect } from "react";

interface RenameDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  file: FileItem;
  onRename: (newName: string) => void;
}

export const RenameDialog = ({
  open,
  onOpenChange,
  file,
  onRename
}: RenameDialogProps) => {
  
  const [newFileName, setNewFileName] = useState('');

  useEffect(() => {
    if (open) {
      setNewFileName(file.name);
    }
  }, [open, file.name]);

  const handleConfirmRename = () => {
    
    if (newFileName.trim() && newFileName !== file.name) {
      onRename(newFileName);
      onOpenChange(false);
    }
  };

  if (!open) {
    return null;
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange} >
      <DialogContent className="no-clear-select">
        <DialogHeader>
          <DialogTitle>Rename {file.type}</DialogTitle>
          <DialogDescription>
            Enter a new name for {file.name}
          </DialogDescription>
        </DialogHeader>
        <Input
          value={newFileName}
          onChange={(e) => {
            setNewFileName(e.target.value);
          }}
          placeholder="New name"
        />
        <DialogFooter>
          <Button variant="secondary" onClick={() => {
            
            onOpenChange(false);
          }}>
            Cancel
          </Button>
          <Button 
            onClick={handleConfirmRename}
            disabled={!newFileName.trim() || newFileName === file.name}
          >
            Rename
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}; 