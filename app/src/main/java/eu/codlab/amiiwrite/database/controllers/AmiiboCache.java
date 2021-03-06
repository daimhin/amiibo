package eu.codlab.amiiwrite.database.controllers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.util.List;
import java.util.UUID;

import eu.codlab.amiiwrite.amiibo.AmiiboMethods;
import eu.codlab.amiiwrite.database.models.Amiibo;
import eu.codlab.amiiwrite.database.models.Amiibo$Table;
import eu.codlab.amiiwrite.utils.IO;

/**
 * Created by kevinleperf on 31/12/15.
 */
public class AmiiboCache extends AbstractCacheable<Long, Amiibo> {


    public List<Amiibo> getAmiibosUnsynced() {
        List<Amiibo> amiibos = new Select()
                .from(Amiibo.class)
                .where(Condition.CombinedCondition
                        .begin(Condition.column(Amiibo$Table.SYNCED).eq(false))
                        .or(Condition.column(Amiibo$Table.SYNCED).isNull()))
                .queryList();

        for (Amiibo amiibo : amiibos) updateCache(amiibo, true);

        return amiibos;
    }

    @NonNull
    public List<Amiibo> getAmiibos(String amiibo_identifier) {
        amiibo_identifier = amiibo_identifier.toLowerCase();
        List<Amiibo> amiibos = new Select()
                .from(Amiibo.class)
                .where(Condition.column(Amiibo$Table.AMIIBO_IDENTIFIER).eq(amiibo_identifier))
                .orderBy(false, Amiibo$Table.CREATED_AT)
                .queryList();

        for (Amiibo amiibo : amiibos) updateCache(amiibo, true);

        return amiibos;
    }

    @NonNull
    public Amiibo getAmiibo(String amiibo_uuid) {
        amiibo_uuid = amiibo_uuid.toLowerCase();
        Amiibo amiibo = new Select()
                .from(Amiibo.class)
                .where(Condition.column(Amiibo$Table.UUID).eq(amiibo_uuid))
                .querySingle();

        if (amiibo != null)
            updateCache(amiibo, true);

        return amiibo;
    }

    @Nullable
    @Override
    protected Amiibo getFromKeyInternal(@NonNull Long id) {
        return new Select()
                .from(Amiibo.class)
                .where(Condition.column(Amiibo$Table.ID).eq(id))
                .querySingle();
    }

    @Override
    protected void updateCache(@NonNull Amiibo object, boolean force) {
        if (null == getCache().get(object.id) || force)
            getCache().put(object.id, object);
    }

    @Override
    @NonNull
    protected Amiibo updateInDatabaseInternal(@NonNull Amiibo object) {
        byte[] identifiers = AmiiboMethods.amiiboIdentifier(object.data.getBlob());

        //set the identifier as the readable representation from the available pages
        object.amiibo_identifier = IO.byteArrayToHexString(identifiers);
        //lower case the amiibo identifier to make research for more efficient
        object.amiibo_identifier = object.amiibo_identifier.toLowerCase();

        //create and uuid from the amiibo, to make easier share/cloud storage
        if (object.uuid == null) object.uuid = UUID.randomUUID().toString();

        object.update();

        return object;
    }

    public void setUploaded(String uuid, boolean b) {
        Amiibo amiibo = getAmiibo(uuid);
        amiibo.synced = b;
        updateAmiibo(amiibo);
    }


    public void updateAmiibo(Amiibo amiibo) {
        byte[] identifiers = AmiiboMethods.amiiboIdentifier(amiibo.data.getBlob());

        //set the identifier as the readable representation from the available pages
        amiibo.amiibo_identifier = IO.byteArrayToHexString(identifiers);
        //lower case the amiibo identifier to make research for more efficient
        amiibo.amiibo_identifier = amiibo.amiibo_identifier.toLowerCase();

        //create and uuid from the amiibo, to make easier share/cloud storage
        if (amiibo.uuid == null) amiibo.uuid = UUID.randomUUID().toString();

        amiibo.update();
        updateCache(amiibo, true);
    }


}
