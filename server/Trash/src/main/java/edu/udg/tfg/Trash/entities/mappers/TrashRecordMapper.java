package edu.udg.tfg.Trash.entities.mappers;

import edu.udg.tfg.Trash.controllers.responses.TrashResponse;
import edu.udg.tfg.Trash.entities.TrashRecord;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrashRecordMapper {

    List<TrashResponse> map(List<TrashRecord> destination);

    TrashResponse map(TrashRecord source);
    TrashRecord map(TrashResponse source);
}
