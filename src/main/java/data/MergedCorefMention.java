package data;

import persistence.ISQLObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MergedCorefMention implements ISQLObject<MergedCorefMention> {
    private WECCoref coref;
    private WECMention mention;

    public MergedCorefMention() {
    }

    public MergedCorefMention(WECCoref coref, WECMention mention) {
        this.coref = coref;
        this.mention = mention;
    }

    @Override
    public String getColumnNames() {
        return null;
    }

    @Override
    public String getColumnNamesAndValues() {
        return null;
    }

    @Override
    public String getValues() {
        return null;
    }

    @Override
    public String getTableName() {
        return null;
    }

    @Override
    public void setPrepareInsertStatementValues(PreparedStatement statement) throws SQLException {

    }

    @Override
    public String getPrepareInsertStatementQuery() {
        return null;
    }

    @Override
    public MergedCorefMention resultSetToObject(ResultSet rs) throws SQLException {
        WECMention mention = new WECMention().resultSetToObject(rs);
        WECCoref coref = new WECCoref("-1").resultSetToObject(rs);

        return new MergedCorefMention(coref, mention);
    }

    public WECCoref getCoref() {
        return coref;
    }

    public WECMention getMention() {
        return mention;
    }
}
