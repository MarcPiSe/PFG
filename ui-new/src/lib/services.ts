import { authService, fileService, sharingService, trashService, userService, adminService } from './api';

export const services = {
    authService: authService ,
    fileService: fileService ,
    sharingService: sharingService ,
    trashService: trashService ,
    //accessControlService: accessControlService ,
    userService: userService ,
    adminService: adminService
};

export const {
    authService: authServiceInstance,
    fileService: fileServiceInstance,
    sharingService: sharingServiceInstance,
    trashService: trashServiceInstance,
    //accessControlService: accessControlServiceInstance,
    userService: userServiceInstance,
    adminService: adminServiceInstance
} = services; 

export { adminServiceInstance as adminService }; 