package com.moyuan.service;

import com.moyuan.entity.Rhyme;
import java.util.List;

public interface RhymeService {

    List<Rhyme> queryByCharacter(String character);

    List<Rhyme> queryByRhymeGroup(String rhymeGroup);
}
