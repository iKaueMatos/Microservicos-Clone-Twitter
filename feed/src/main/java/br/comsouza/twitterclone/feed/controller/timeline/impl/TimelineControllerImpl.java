package br.comsouza.twitterclone.feed.controller.timeline.impl;

import br.comsouza.twitterclone.feed.controller.timeline.ITimelineController;
import br.comsouza.twitterclone.feed.dto.posts.TimelineTweetResponse;
import br.comsouza.twitterclone.feed.service.timeline.ITimelineService;
import br.comsouza.twitterclone.feed.util.FindUserIdentifierHelper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/timeline")
public class TimelineControllerImpl implements ITimelineController {

    private final ITimelineService iTimelineService;

    public TimelineControllerImpl(ITimelineService iTimelineService) {
        this.iTimelineService = iTimelineService;
    }

    @GetMapping(value = "/following", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TimelineTweetResponse>> getFollowingTimeline(@RequestParam(value = "page", required = true) Integer page,
                                                                            @RequestParam(value = "size", required = true) Integer size) throws Exception{
        return new ResponseEntity<>(iTimelineService.getFollowingTimeline(FindUserIdentifierHelper.getIdentifier(), page, size), HttpStatus.OK);
    }

    //TODO: Fazer FOR YOU (postagem/retweet/comment de quem eu sigo e de quem os segue)
}
