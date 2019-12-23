package org.springframework.data.gigaspaces.examples.advanced.transaction;

import org.springframework.data.gigaspaces.examples.model.MeetingRoom;
import org.springframework.data.gigaspaces.repository.GigaspacesRepository;

/**
 * @author Anna_Babich.
 */
public interface MeetingRoomRepository extends GigaspacesRepository<MeetingRoom, String> {
}
