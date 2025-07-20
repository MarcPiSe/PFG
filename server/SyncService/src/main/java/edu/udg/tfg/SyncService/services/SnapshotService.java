package edu.udg.tfg.SyncService.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.codec.digest.DigestUtils;
import edu.udg.tfg.SyncService.entities.SnapshotEntity;
import edu.udg.tfg.SyncService.entities.SnapshotElementEntity;
import edu.udg.tfg.SyncService.repositories.SnapshotElementRepository;
import edu.udg.tfg.SyncService.repositories.SnapshotRepository;
import edu.udg.tfg.SyncService.queue.messages.CommandRabbit;
import java.util.UUID;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SnapshotService {

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Autowired
    private SnapshotElementRepository snapshotElementRepository;

    public SnapshotEntity addRoot(UUID userId, UUID elementId) {
        SnapshotEntity snapshot = snapshotRepository.findByUserId(userId);
        if (snapshot == null) {
            snapshot = new SnapshotEntity();
            snapshot.setUserId(userId);
        }
        SnapshotElementEntity element = new SnapshotElementEntity();
        element.setElementId(elementId);
        element.setParent(null);
        element.setType("folder");
        element.setName("/");
        element.setPath("/");
        
        if (element.getContent() == null) {
            element.setContent(new ArrayList<>());
        }
        
        element.setHash(calculateFolderHash(element));
        
        snapshotElementRepository.save(element);
        snapshot.getElements().add(element);
        SnapshotEntity savedSnapshot = snapshotRepository.save(snapshot);
        element.setSnapshot(savedSnapshot);
        snapshotElementRepository.save(element);
        return savedSnapshot;
    }

    @Transactional
    public SnapshotEntity processCommand(CommandRabbit command) {
        try {
            SnapshotEntity snapshot = snapshotRepository.findByUserId(UUID.fromString(command.userId()));
            if (snapshot == null || snapshot.getElements() == null) {
                snapshot = addRoot(UUID.fromString(command.userId()), UUID.fromString(command.elementId()));
            }
            SnapshotElementEntity targetElement = snapshotElementRepository.findByElementId(UUID.fromString(command.elementId())).orElse(null);
            String parentId = command.parentId();
            String elementId = snapshot.getId().toString();
            SnapshotElementEntity parent = snapshotElementRepository.findBySnapshotIdAndElementId(snapshot.getId(), UUID.fromString(command.parentId())).orElse(null);

            if (command.action().equals("delete")) {
                snapshot = deleteElement(targetElement, parent, snapshot);
            } else {
                if(parent == null) {
                    throw new RuntimeException("Parent element not found");
                }
                if (targetElement == null) {
                    snapshot = addElement(command, parent, snapshot);
                } else {
                    snapshot = modifyElement(command, targetElement, parent, snapshot);
                }
            }
            
            recalculateHash(parent);
            
            return snapshot;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Transactional
    private SnapshotEntity deleteElement(SnapshotElementEntity element, SnapshotElementEntity parent, SnapshotEntity snapshot) {
        if (element == null) {
            return snapshot;
        }
        
        if (parent != null && parent.getContent() != null) {
            parent.getContent().removeIf(e -> e.getElementId().equals(element.getElementId()));
            snapshotElementRepository.save(parent);
        }
        
        if (snapshot != null && snapshot.getElements() != null) {
            snapshot.getElements().removeIf(e -> e.getElementId().equals(element.getElementId()));
        }
        
        element.setParent(null);
        element.setSnapshot(null);
        
        snapshotElementRepository.save(element);
        
        snapshotElementRepository.delete(element);
        snapshotElementRepository.flush();
        
        return snapshotRepository.save(snapshot);
    }

    private SnapshotEntity addElement(CommandRabbit command, SnapshotElementEntity parent, SnapshotEntity snapshot) {
        SnapshotElementEntity newElement = new SnapshotElementEntity();
        newElement.setElementId(UUID.fromString(command.elementId()));
        newElement.setParent(parent);
        newElement.setType(command.type());
        newElement.setName(command.name());
        newElement.setPath(command.path());
        newElement.setSnapshot(snapshot);

        
        if ("folder".equals(command.type())) {
            newElement.setContent(new ArrayList<>());
        }
        
        String hash = calculateElementHash(newElement, command.hash());
        newElement.setHash(hash);
        
        newElement = snapshotElementRepository.save(newElement);
        snapshot.getElements().add(newElement);
        
        if (parent.getContent() == null) {
            parent.setContent(new ArrayList<>());
        }
        parent.getContent().add(newElement);
        
        snapshotElementRepository.flush();
        
        return snapshotRepository.save(snapshot);
    }

    private SnapshotEntity modifyElement(CommandRabbit command, SnapshotElementEntity element, SnapshotElementEntity newParent, SnapshotEntity snapshot) {
        SnapshotElementEntity oldParent = element.getParent();
        if (oldParent != null && !oldParent.equals(newParent)) {
            oldParent.getContent().remove(element);
            recalculateHash(oldParent);
        }
        
        element.setPath(command.path());
        element.setType(command.type());
        element.setName(command.name());
        element.setParent(newParent);
        
        if ("folder".equals(command.type()) && element.getContent() == null) {
            element.setContent(new ArrayList<>());
        }
        
        String hash = calculateElementHash(element, command.hash());
        element.setHash(hash);
        
        if (newParent.getContent() == null) {
            newParent.setContent(new ArrayList<>());
        }
        newParent.getContent().add(element);
        
        snapshotElementRepository.flush();
        return snapshotRepository.save(snapshot);
    }

    private String calculateElementHash(SnapshotElementEntity element, String hash) {
        if ("folder".equals(element.getType())) {
            return calculateFolderHash(element);
        } else {
            if (hash != null && !hash.trim().isEmpty()) {
                return hash;
            } else {
                throw new RuntimeException("Hash not provided for file " + element.getName());
            }
        }
    }

    private String calculateFolderHash(SnapshotElementEntity folder) {
        if (folder.getContent() == null || folder.getContent().isEmpty()) {
            return DigestUtils.sha256Hex("");
        }
        StringBuilder elementHash = new StringBuilder();
        
        for (SnapshotElementEntity content : folder.getContent().stream().sorted(Comparator.comparing(SnapshotElementEntity::getName)).toList()) {
            if (content.getHash() != null) {
                elementHash.append(content.getHash());
            }
        }
        
        return DigestUtils.sha256Hex(elementHash.toString());
    }

    private void recalculateHash(SnapshotElementEntity element) {
        if (element != null && "folder".equals(element.getType())) {
            String newHash = calculateFolderHash(element);
            element.setHash(newHash);
            snapshotElementRepository.save(element);
            
            if (element.getParent() != null) {
                recalculateHash(element.getParent());
            }
        }
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        snapshotElementRepository.deleteBySnapshotUserId(userId);
        snapshotRepository.deleteByUserId(userId);
    }

    public SnapshotEntity getSnapshotByUserId(UUID userId) {
        return snapshotRepository.findByUserId(userId);
    }
}
