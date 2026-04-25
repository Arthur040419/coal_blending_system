package com.coalblend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.common.result.Result;
import com.coalblend.entity.MineSource;
import com.coalblend.mapper.MineSourceMapper;
import com.coalblend.service.chain.BatchNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mineSource")
@RequiredArgsConstructor
public class MineSourceController {

    private final MineSourceMapper mineSourceMapper;
    private final BatchNoGenerator batchNoGenerator;

    @GetMapping("/page")
    public Result<IPage<MineSource>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<MineSource> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(MineSource::getSourceCode, keyword)
                    .or().like(MineSource::getMineArea, keyword)
                    .or().like(MineSource::getMineName, keyword)
                    .or().like(MineSource::getWorkingFace, keyword));
        }
        if (status != null) {
            w.eq(MineSource::getStatus, status);
        }
        w.orderByDesc(MineSource::getId);
        return Result.ok(mineSourceMapper.selectPage(new Page<>(current, size), w));
    }

    @GetMapping("/detail/{id}")
    public Result<MineSource> detail(@PathVariable Long id) {
        MineSource row = mineSourceMapper.selectById(id);
        if (row == null) throw new BusinessException(404, "矿区来源不存在");
        return Result.ok(row);
    }

    @PostMapping("/add")
    public Result<MineSource> add(@RequestBody MineSource body) {
        if (!StringUtils.hasText(body.getMineArea())) throw new BusinessException("矿区不能为空");
        if (!StringUtils.hasText(body.getMineName())) throw new BusinessException("矿井不能为空");
        if (!StringUtils.hasText(body.getSourceCode())) body.setSourceCode(batchNoGenerator.mineSourceCode());
        if (body.getStatus() == null) body.setStatus(1);
        mineSourceMapper.insert(body);
        return Result.ok(body);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody MineSource body) {
        if (body.getId() == null) throw new BusinessException("id不能为空");
        mineSourceMapper.updateById(body);
        return Result.ok();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        MineSource patch = new MineSource();
        patch.setId(id);
        patch.setStatus(0);
        mineSourceMapper.updateById(patch);
        return Result.ok();
    }
}
