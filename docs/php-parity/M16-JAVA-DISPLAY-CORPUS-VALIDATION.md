# M16 Java解码全量模板对照（专项验证）

本机隔离库只读取得704份报告模板及2份邮件模板的原始文本，以冻结PHP common.php的原decodeUnicode纯函数产生预期结果，再用实际Java LegacyDictionarySelector.display逐份比较。原PHP编辑视图确实在编辑器前调用该函数，未用自编预期替代原行为。源码hash见java-display-corpus-parity.json。

验证由LegacyDisplayCorpusTest执行，实际1项JUnit测试循环706个模板案例，失败/错误/跳过均0，不能称为706项JUnit测试。故意给错预期的合成负对照确实触发“corpus index 0”断言失败（/tmp/petlis-java-display-kjupqlq6.log），不是因编译错误或未执行而“拒绝”。正式正对照日志/tmp/petlis-java-display-oj3w0xlf.log。

原文与PHP结果只放在本机Petlis私有目录的临时0600文件；JUnit不加载Spring/应用配置、不连接数据库、不发网络请求；断言只输出案例序号，避免正文进入Surefire。每次使用随机nonce证明测试实际读取本次数据并比较完全部案例，结果计数及源码hash再次校验。临时原文与证明文件均按精确路径在finally删除；复查残留0。日志和仓库结果仅含计数、hash与路径。

复现：从项目根运行python3 scripts/local/probe-legacy-editor-corpus.py --browser --hierarchy --php-display --java-display。参考PHP仅在已核实的独立Petlis容器中运行提取的纯函数，不修改PHP或覆盖层；本次未改库结构或业务数据。

同批原PHP编辑输入的Chrome结果：706份文字与所检查的文字计算样式一致、无解析异常；结构704通过/2份异常属性差异，详见editor-browser-display-hierarchy-probe.json。该结果没有证明完整模板CRUD、工具栏、实际编辑后报告/PDF输出、全链路或逐模块两轮复核已完成。模块仍为partial，产品编辑器保存尚未接入。
