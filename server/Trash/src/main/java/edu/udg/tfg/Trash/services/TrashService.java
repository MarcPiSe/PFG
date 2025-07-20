package edu.udg.tfg.Trash.services;

import com.netflix.discovery.converters.Auto;
import edu.udg.tfg.Trash.controllers.requests.TrashRequest;
import edu.udg.tfg.Trash.controllers.responses.TrashDetailsResponse;
import edu.udg.tfg.Trash.controllers.responses.TrashResponse;
import edu.udg.tfg.Trash.entities.RecordStatus;
import edu.udg.tfg.Trash.entities.TrashRecord;
import edu.udg.tfg.Trash.entities.enums.AccessType;
import edu.udg.tfg.Trash.feignClients.responses.AccessResponse;
import org.springframework.web.server.ResponseStatusException;  
import edu.udg.tfg.Trash.feignClients.FileAccessControlClient;
import edu.udg.tfg.Trash.feignClients.FileManagementClient;
import edu.udg.tfg.Trash.queue.Sender;
import edu.udg.tfg.Trash.repositories.TrashRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import edu.udg.tfg.Trash.feignClients.requests.SetDeletedRequest;
import edu.udg.tfg.Trash.feignClients.responses.SetDeletedResponse;
import org.springframework.transaction.annotation.Transactional;
import edu.udg.tfg.Trash.controllers.responses.FolderInfo;
import edu.udg.tfg.Trash.controllers.responses.FileInfo;
import edu.udg.tfg.Trash.services.CommandService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TrashService {

    @Autowired
    private TrashRecordRepository trashRecordRepository;

    @Autowired
    private FileManagementClient fileManagementClient;

    @Autowired
    private FileAccessControlClient fileAccessControlClient;

    @Autowired
    private CommandService commandService;

    @Value("${access.service.name}")
    private String accessName;
    @Value("${sharing.service.name}")
    private String sharingName;
    @Value("${management.service.name}")
    private String managementName;

    @Autowired
    private Sender sender;

    public void addRecord(UUID userId, TrashRequest trashRequest) {
        updateOrCreateRecord(userId, trashRequest.getElementId(), true);
    
        trashRequest.getIds().forEach(childId -> {
            updateOrCreateRecord(userId, childId, false);
        });
    }
    
    private void updateOrCreateRecord(UUID userId, UUID elementId, boolean isRoot) {
        Optional<TrashRecord> existingRecord = trashRecordRepository.findByElementIdAndUserId(elementId, userId);
    
        TrashRecord record;
        if (existingRecord.isPresent()) {
            record = existingRecord.get();
            record.setRoot(isRoot);
            record.setDeletionDate(new Date());
        } else {
            record = new TrashRecord(userId, elementId, isRoot);
        }
        trashRecordRepository.save(record);
    }

    public TrashResponse getSharedFiles(UUID userId) {
        List<TrashRecord> records = trashRecordRepository.findByUserIdAndRoot(userId, true);
        TrashResponse trashResponse = new TrashResponse();
        records.forEach(r -> trashResponse.getFiles().add(r.getElementId()));
        return trashResponse;
    }

    public TrashDetailsResponse getTrashFile(UUID userId, UUID elementId) {
        TrashRecord trashRecord = trashRecordRepository.findByElementIdAndUserId(elementId, userId).orElseThrow(() ->new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        TrashDetailsResponse trashDetailsResponse = new TrashDetailsResponse();
        trashDetailsResponse.setDeletedDate(trashRecord.getDeletionDate());
        trashDetailsResponse.setExpirationDate(trashRecord.getExpirationDate());
        return trashDetailsResponse;
    }

    public void confirm(UUID elementId, UUID userId, String service) {
        TrashRecord trashRecord = trashRecordRepository.findByElementIdAndUserId(elementId, userId).orElseThrow(() ->new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (service.equals(accessName)) {
            trashRecord.setAccess(true);
        } else if(service.equals(sharingName)) {
            trashRecord.setSharing(true);
        } else if(service.equals(managementName)) {
            trashRecord.setManager(true);
        }
        trashRecordRepository.save(trashRecord);
    }

    public void cleanRecords() {
        trashRecordRepository.clearPending();
    }

    public void remove(UUID userId, UUID elementId) {
        trashRecordRepository.deleteByElementIdAndUserId(userId, elementId);
    }

    public void removeBulk(UUID userId, List<UUID> elementIds) {
        trashRecordRepository.deleteByUserIdAndElementIdIn(userId, elementIds);
    }

    public void deleteByUserId(UUID userId) {
        trashRecordRepository.deleteByUserId(userId);
    }

    public List<TrashRecord> findByUserId(UUID userId) {
        return trashRecordRepository.findByUserId(userId);
    }

    public void moveToTrash(UUID userId, UUID connectionId, UUID elementId) {
        try {
            AccessResponse accessResponse = fileAccessControlClient.getFileAccess(elementId, userId);
            if (accessResponse.getAccessType() <= AccessType.ADMIN.ordinal()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have WRITE access to the element.");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission check failed for the element");
        }

        SetDeletedRequest request = new SetDeletedRequest(true);
        SetDeletedResponse response;
        try {
            response = fileManagementClient.setElementDeletedState(userId, connectionId, elementId, request);
            if (response == null || response.getElementIds() == null || response.getElementIds().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to move element to trash");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error communicating with file management service");
        }

        List<UUID> affectedIds = response.getElementIds();
        for (UUID affectedId : affectedIds) {
            boolean isRoot = affectedId.equals(elementId);
            updateOrCreateRecord(userId, affectedId, isRoot);
        }
    }

    public void restoreFromTrash(UUID userId, UUID connectionId, UUID elementId) {
        try {
            AccessResponse accessResponse = fileAccessControlClient.getFileAccess(elementId, userId);
            if (accessResponse.getAccessType() < AccessType.WRITE.ordinal()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have WRITE access to the element.");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission check failed for the element");
        }
        
        TrashRecord trashRecord = trashRecordRepository.findByElementId(elementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Element not found in trash"));

        if (!trashRecord.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have permission to restore this element");
        }

        SetDeletedRequest request = new SetDeletedRequest(false);
        try {
            SetDeletedResponse response = fileManagementClient.setElementDeletedState(userId, connectionId, elementId, request);
            if (response == null || response.getElementIds() == null || response.getElementIds().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to restore element from trash");
            }
            trashRecordRepository.deleteByUserIdAndElementIdIn(userId, response.getElementIds());

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error communicating with file management service");
        }
    }

    public void deletePermanently(UUID userId, UUID elementId) {
        TrashRecord trashRecord = trashRecordRepository.findByElementId(elementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Element not found in trash"));

        if (!trashRecord.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have permission to delete this element");
        }
        try {
            SetDeletedResponse response = fileManagementClient.deleteElementPermanently(elementId);
             if (response == null || response.getElementIds() == null || response.getElementIds().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to permanently delete element");
            }
            List<TrashRecord> trashRecords = trashRecordRepository.findByUserIdAndElementIdIn(userId, response.getElementIds());
            trashRecords.forEach(record -> {
                record.setStatus(RecordStatus.PENDING_DELETION);
                record.setManager(true);
                record.setAccess(false);
                record.setSharing(false);
            });
            trashRecordRepository.saveAll(trashRecords);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error communicating with file management service");
        }
    }
    
    public FolderInfo getTrashRoot(UUID userId) {
        List<TrashRecord> trashRecords = trashRecordRepository.findByUserIdAndStatus(userId, RecordStatus.ACTIVE);
        List<UUID> trashElementIds = trashRecords.stream()
                .map(TrashRecord::getElementId)
                .collect(Collectors.toList());

        Map<UUID, TrashRecord> map = new HashMap<>();
        for (TrashRecord record : trashRecords) {
            map.put(record.getElementId(), record);
        }

        if (trashRecords.isEmpty()) {
            return new FolderInfo();
        }

        FolderInfo folderInfo = fileManagementClient.getFolderStructure(trashElementIds, true);
        setDates(folderInfo, map);
        return folderInfo;
    }

    private void setDates(FolderInfo folderInfo, Map<UUID, TrashRecord> map) {
        for (FileInfo file : folderInfo.getFiles()) {
            file.setDeletedAt(map.get(file.getId()).getDeletionDate());
            file.setExpirationDate(map.get(file.getId()).getExpirationDate());
        }
        for (FolderInfo folder : folderInfo.getSubfolders()) {
            folder.setDeletedAt(map.get(folder.getId()).getDeletionDate());
            folder.setExpirationDate(map.get(folder.getId()).getExpirationDate());
        }
    }

    public void markExpiredAsPending() {
        List<TrashRecord> expiredRecords = trashRecordRepository
                .findByExpirationDateLessThanEqualAndStatus(new Date(), RecordStatus.ACTIVE);
        
        for (TrashRecord record : expiredRecords) {
            record.setStatus(RecordStatus.PENDING_DELETION);
            trashRecordRepository.save(record);
        }
    }

    public void processPendingDeletions() {
        List<TrashRecord> pendingRecords = trashRecordRepository.findByStatus(RecordStatus.PENDING_DELETION);
        
        for (TrashRecord record : pendingRecords) {
            if(!record.isAccess() && !record.isManager() && !record.isSharing()) {
                commandService.sendTrashClean(record.getUserId().toString(), record.getElementId().toString());
            }
            if(!record.isAccess()) { 
                sender.removeAccess(record.getUserId(), record.getElementId());
            }
            if(!record.isManager()) { 
                sender.removeManagement(record.getUserId(), record.getElementId());
            }
            if(!record.isSharing()) { 
                sender.removeSharing(record.getUserId(), record.getElementId());
            }
        }
    }
}