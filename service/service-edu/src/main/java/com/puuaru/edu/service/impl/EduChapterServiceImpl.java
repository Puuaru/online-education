package com.puuaru.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puuaru.edu.entity.EduChapter;
import com.puuaru.edu.entity.EduVideo;
import com.puuaru.edu.mapper.EduChapterMapper;
import com.puuaru.edu.service.EduChapterService;
import com.puuaru.edu.service.EduVideoService;
import com.puuaru.edu.vo.ChapterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 课程 服务实现类
 * </p>
 *
 * @author puuaru
 * @since 2022-12-05
 */
@Service
public class EduChapterServiceImpl extends ServiceImpl<EduChapterMapper, EduChapter> implements EduChapterService {

    @Autowired
    EduVideoService videoService;

    /**
     * 将连同小节信息的整个章节信息删除
     * @param chapterId
     */
    @Override
    public void deleteById(Long chapterId) {
        // wrapper to find videos with chapterId
        // delete videos' records with wrapper
        QueryWrapper<EduVideo> wrapper = new QueryWrapper<>();
        wrapper.eq("chapter_id", chapterId);
        videoService.remove(wrapper);

        // (?) delete videos from remote server

        // delete chapter by chapterId
        super.removeById(chapterId);
    }

    /**
     * 获取课程对应的所有章节和各章节对应的小节
     * @param courseId
     * @return
     */
    @Override
    public List<ChapterVO> getCourseDetails(Long courseId) {
        QueryWrapper<EduChapter> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId);
        List<EduChapter> chapters = super.list(wrapper);

        return chapters.stream()
                .map(item -> new ChapterVO(item.getId(), item.getTitle(), null))
                .peek(item -> {
                    QueryWrapper<EduVideo> videoWrapper = getChildrenWrapper(item);
                    item.setChildren(getVideoList(videoService.list(videoWrapper)));
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取查询子节点的wrapper，在chapterService中，视Chapter实体类和Video实体类的chapterId字段为父节点字段
     * 从而保证可能在Video中进行更多的细分
     *
     * @param item
     * @return
     */
    private QueryWrapper<EduVideo> getChildrenWrapper(ChapterVO item) {
        QueryWrapper<EduVideo> videoWrapper = new QueryWrapper<>();
        videoWrapper.eq("chapter_id", item.getId());
        return videoWrapper;
    }

    /**
     * 递归查找作为子节点的视频（小节）列表
     *
     * @param videos
     * @return
     */
    private List<ChapterVO> getVideoList(List<EduVideo> videos) {
        return videos.stream()
                // 将Video实体类转换为ChapterVO类时会存放关于Video的更多信息
                .map(item -> new ChapterVO(item.getId(), item.getTitle(), null))
                .peek(item -> {
                    // 查询每个视频是否有分p
                    QueryWrapper<EduVideo> videoWrapper = getChildrenWrapper(item);
                    item.setChildren(getVideoList(videoService.list(videoWrapper)));
                })
                .collect(Collectors.toList());
    }
}
