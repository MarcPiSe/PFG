package edu.udg.tfg.FileAccessControl.services;// FileAccessService.java

import edu.udg.tfg.FileAccessControl.entities.AccessRule;
import edu.udg.tfg.FileAccessControl.feignClients.UserManagement.User;
import edu.udg.tfg.FileAccessControl.controllers.responses.AccessResponse;
import edu.udg.tfg.FileAccessControl.repositories.AccessRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import edu.udg.tfg.FileAccessControl.entities.mappers.AccessMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;  
import edu.udg.tfg.FileAccessControl.feignClients.UserManagement.UserManagementClient;
import edu.udg.tfg.FileAccessControl.feignClients.UserManagement.User;
import edu.udg.tfg.FileAccessControl.feignClients.FileManagementClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileAccessService {

    @Autowired
    private AccessRuleRepository accessRuleRepository;

    @Autowired
    private AccessMapper accessMapper;

    @Autowired
    private UserManagementClient userManagementClient;

    @Autowired
    private FileManagementClient fileManagementClient;

    @Transactional
    public void addFileAccess(AccessRule fileAccess) {
        if (fileAccess == null || fileAccess.getElementId() == null || fileAccess.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AccessRule and its properties elementId and userId cannot be null");
        }
        
        Optional<AccessRule> existingAccess = accessRuleRepository.findByElementIdAndUserId(fileAccess.getElementId(), fileAccess.getUserId());
        if (existingAccess.isPresent()) {
            AccessRule accessRule = existingAccess.get();
            accessRule.setAccessType(fileAccess.getAccessType());
            accessRuleRepository.save(accessRule);
        } else {
            accessRuleRepository.save(fileAccess);
        }
    }

    @Transactional
    public void addFileAccessList(List<AccessRule> list) {
        for(AccessRule accessRule : list) {
            if (accessRule == null || accessRule.getElementId() == null || accessRule.getUserId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AccessRule and its properties elementId and userId cannot be null");
            }
            Optional<AccessRule> existingAccess = accessRuleRepository.findByElementIdAndUserId(accessRule.getElementId(), accessRule.getUserId());
            if (existingAccess.isPresent()) {
                AccessRule existent = existingAccess.get();
                existent.setAccessType(accessRule.getAccessType());
                accessRuleRepository.save(existent);
            } else {
                accessRuleRepository.save(accessRule);
            }
        }
    }

    @Transactional
    public void updateAccess(AccessRule fileAccess) {
        if (fileAccess == null || fileAccess.getElementId() == null || fileAccess.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AccessRule and its properties elementId and userId cannot be null");
        }
        AccessRule existingAccess = accessRuleRepository.findByElementIdAndUserId(fileAccess.getElementId(), fileAccess.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Access rule not found"));
        existingAccess.setAccessType(fileAccess.getAccessType());
        accessRuleRepository.save(existingAccess);
    }

    @Transactional
    public void deleteFileAccess(UUID fileId, UUID userId) {
        if (fileId == null || userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileId and userId cannot be null");
        }
        accessRuleRepository.deleteByElementIdAndUserId(fileId, userId);
    }

    public AccessRule getFileAccess(UUID elementId, UUID userId, boolean always) {
        if (elementId == null || userId == null) {
            return new AccessRule(); 
        }
        
        Optional<AccessRule> directAccess = accessRuleRepository.findByElementIdAndUserId(elementId, userId);
        if (directAccess.isPresent()) {
            return directAccess.get();
        }
        
        if(!always) {
            return new AccessRule();
        }
        try {
            UUID parentId = fileManagementClient.getParentId(elementId);
            if (parentId == null) {
                return new AccessRule(); 
            }
            return getFileAccess(parentId, userId, always);
        } catch (Exception e) {
            return new AccessRule(); 
        }
    }

    public List<AccessRule> getFileAccessByUserId(UUID userId) {
        return accessRuleRepository.findByUserId(userId);
    }

    public int getFileAccessType(UUID fileId, UUID userId) {
        AccessRule accessRule = getFileAccess(fileId, userId, true);
        return accessRule.getAccessType().ordinal();
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        accessRuleRepository.deleteByUserId(userId);
    }

    @Transactional
    public void deleteAllAccessForElement(String elementId) {
        accessRuleRepository.deleteByElementId(UUID.fromString(elementId));
    }
}
